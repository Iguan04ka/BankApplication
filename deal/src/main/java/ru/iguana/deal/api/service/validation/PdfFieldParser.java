package ru.iguana.deal.api.service.validation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбирает «сырой» текст справки 2-НДФЛ и выписки СТД-Р, извлекая
 * значимые поля для сравнения с данными заявки.
 *
 * <p>Регулярные выражения откалиброваны под реальные форматы:
 * <ul>
 *   <li>2-НДФЛ (форма КНД 1175018): «Фамилия Иванов Имя Аркадий Отчество*
 *       Алексеевич», «Дата рождения 23.09.1977», «Серия и номер документа
 *       77 01 526805», таблица «MM КОД СУММА» без года в строке;</li>
 *   <li>СТД-Р: ФИО построчно, «Отчество (при наличии) Алексеевич»,
 *       «Дата рождения «23 » 09 1977», записи «20.08.2018 ПРИЕМ ...».</li>
 * </ul>
 *
 * <p>Все методы устойчивы к отсутствию полей — в этом случае возвращают
 * {@code Optional.empty()} или пустой список. Решение о том, что считать
 * ошибкой, принимает {@code DocumentValidationService}.
 */
@Component
@Slf4j
public class PdfFieldParser {

    // ── ФИО ──────────────────────────────────────────────────────────────────
    // ВАЖНО: \b в Java по умолчанию не считает кириллические буквы word-символами,
    // поэтому для границы слова используем lookbehind/lookahead с \p{L}.
    // Это гарантирует, что «Имя» не зацепится как подстрока «Имярек».
    private static final String BOUNDARY_BEFORE = "(?<![\\p{L}])";
    private static final String BOUNDARY_AFTER  = "(?![\\p{L}])";

    /** «Фамилия Иванов», «Фамилия:\nИванов». */
    private static final Pattern P_LAST_NAME = Pattern.compile(
            "(?iu)" + BOUNDARY_BEFORE + "Фамилия" + BOUNDARY_AFTER
                    + "\\s*[:]?\\s*([\\p{L}\\-]{2,})");

    /** «Имя Аркадий», «Имя:\nАркадий». */
    private static final Pattern P_FIRST_NAME = Pattern.compile(
            "(?iu)" + BOUNDARY_BEFORE + "Имя" + BOUNDARY_AFTER
                    + "\\s*[:]?\\s*([\\p{L}\\-]{2,})");

    /**
     * Отчество — самое капризное поле:
     * <ul>
     *   <li>в 2-НДФЛ — «Отчество* Алексеевич» (звёздочка ссылается на сноску);</li>
     *   <li>в СТД-Р — «Отчество (при наличии) Алексеевич»;</li>
     *   <li>иногда — «Отчество: Алексеевич» или просто «Отчество Алексеевич».</li>
     * </ul>
     * Поэтому между ключевым словом и значением допускаем «*»,
     * «(при наличии)», двоеточие и любые пробелы в произвольном порядке.
     */
    private static final Pattern P_MIDDLE_NAME = Pattern.compile(
            "(?iu)" + BOUNDARY_BEFORE + "Отчество" + BOUNDARY_AFTER
                    + "\\s*\\*?\\s*(?:\\(при\\s+наличии\\))?\\s*[:]?\\s*([\\p{L}\\-]{2,})");

    // ── Дата рождения ────────────────────────────────────────────────────────

    /** Точный формат 2-НДФЛ: «Дата рождения 23.09.1977». Быстрая проверка. */
    private static final Pattern P_BIRTH_DATE_DOTTED = Pattern.compile(
            "(?iu)Дата\\s+рождения\\s*[:]?\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");

    /**
     * Гибкий формат, покрывающий СТД-Р («Дата рождения «23 » 09 1977»)
     * и любые иные «нестандартные» представления, в которых PDFBox
     * может:
     * <ul>
     *   <li>вставлять между цифрами пробелы — например, «2 3» / «1 9 7 7»
     *       при извлечении из табличных подчёркнутых полей;</li>
     *   <li>использовать произвольные разделители — кавычки «», точки,
     *       слэши, табы и т.п.;</li>
     *   <li>помещать значение в произвольной позиции внутри 40 символов
     *       после заголовка «Дата рождения».</li>
     * </ul>
     *
     * <p>Группы:
     * <ol>
     *   <li>день — 1 или 2 цифры с опциональным пробелом;</li>
     *   <li>месяц — 1 или 2 цифры с опциональным пробелом;</li>
     *   <li>год — 4 цифры с возможными пробелами.</li>
     * </ol>
     * Перед {@code parseInt} из каждой группы удаляются все пробелы.
     */
    private static final Pattern P_BIRTH_DATE_FLEXIBLE = Pattern.compile(
            "(?iu)Дата\\s+рождения"
                    + "[^\\d]{0,40}?"            // произвольный «мусор» между заголовком и днём
                    + "(\\d(?:\\s*\\d)?)"        // день: 1-2 цифры с возможным пробелом
                    + "[^\\d]{1,10}"             // разделитель (пробелы, «»», «.», ...)
                    + "(\\d(?:\\s*\\d)?)"        // месяц: 1-2 цифры с возможным пробелом
                    + "[^\\d]{1,10}"
                    + "(\\d(?:\\s*\\d){3})");    // год: 4 цифры (между ними допустимы пробелы)

    // ── Паспорт (только 2-НДФЛ) ──────────────────────────────────────────────

    /**
     * «Серия и номер документа 77 01 526805». Серия может быть «7701»
     * или «77 01» — пробел не обязателен. Номер — ровно 6 цифр.
     */
    private static final Pattern P_PASSPORT = Pattern.compile(
            "(?iu)Серия\\s+и\\s+номер\\s+документа\\s*[:]?\\s*(\\d{2}\\s?\\d{2})\\s+(\\d{6})");

    // ── ИНН ──────────────────────────────────────────────────────────────────

    /**
     * 10-значный ИНН организации (для физлица ИНН 12-значный — его берём
     * только если 10-значного не нашли, но в СТД-Р физлицевого ИНН и нет).
     * Граница перед «ИНН» через lookbehind, после числа — отрицательный
     * lookahead на цифру, чтобы не зацепить кусок 12-значного физлицевого ИНН.
     */
    private static final Pattern P_INN_ORGANIZATION = Pattern.compile(
            "(?iu)" + BOUNDARY_BEFORE + "ИНН" + BOUNDARY_AFTER
                    + "[^\\d]{0,20}(\\d{10})(?!\\d)");

    // ── Таблица доходов 2-НДФЛ ───────────────────────────────────────────────

    /**
     * Строка таблицы доходов 2-НДФЛ — формат «MM КОД СУММА»: месяц
     * (01..12) + 4-значный код дохода (2000, 2012, 2300 и т.д.) +
     * сумма с копейками. Год в строке НЕ присутствует — он указан в
     * шапке справки. В одном месяце возможно несколько строк (разные
     * коды дохода: зарплата, отпускные, премия и т.п.) — суммируем их.
     */
    private static final Pattern P_INCOME_ROW = Pattern.compile(
            "(?m)^\\s*(0[1-9]|1[0-2])\\s+(\\d{4})\\s+([\\d\\s]+[,.]\\d{2})\\s*$");

    // ── Записи СТД-Р ─────────────────────────────────────────────────────────

    /**
     * «20.08.2018 ПРИЕМ», «05.09.2022 ПЕРЕВОД», «15.05.2023 УВОЛЬНЕНИЕ».
     * Граница после ключевого слова — отрицательный lookahead на букву,
     * чтобы не цеплять формы вроде «ПРИЕМКА» или «УВОЛЬНЕНИЕМ».
     */
    private static final Pattern P_EMPLOYMENT_RECORD = Pattern.compile(
            "(?iu)(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(ПРИЁМ|ПРИЕМ|ПЕРЕВОД|УВОЛЬНЕНИЕ)(?![\\p{L}])");

    private static final DateTimeFormatter DATE_DOTTED = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // ── Публичные методы парсинга ────────────────────────────────────────────

    /**
     * Нормализует текст: убирает дублирующиеся пробелы, унифицирует переносы.
     * Возвращает копию — исходник остаётся нетронутым.
     *
     * <p>Важно: {@link #P_INCOME_ROW} полагается на {@code ^} и {@code $},
     * поэтому переносы строк намеренно <b>сохраняются</b>; нормализация
     * сжимает только повторяющиеся пробелы/табы в строке.
     */
    public String normalize(String text) {
        if (text == null) return "";
        return text
                .replace(' ', ' ')            // non-breaking space → обычный
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")        // повторяющиеся пробелы/табы
                .trim();
    }

    public Optional<String> parseLastName(String text)   { return findFirstGroup(P_LAST_NAME, text); }
    public Optional<String> parseFirstName(String text)  { return findFirstGroup(P_FIRST_NAME, text); }
    public Optional<String> parseMiddleName(String text) { return findFirstGroup(P_MIDDLE_NAME, text); }

    /**
     * Возвращает дату рождения, пытаясь сначала «точечный» формат
     * (2-НДФЛ — быстрый и строгий), а если не нашли — гибкий формат,
     * который умеет работать с СТД-Р и со «склеенным/разнесённым»
     * выводом PDFBox.
     */
    public Optional<LocalDate> parseBirthDate(String text) {
        // 1) DD.MM.YYYY — точный и быстрый путь для 2-НДФЛ.
        Matcher dotted = P_BIRTH_DATE_DOTTED.matcher(text);
        if (dotted.find()) {
            Optional<LocalDate> result = tryParseDottedDate(dotted.group(1));
            if (result.isPresent()) {
                log.debug("PdfFieldParser: дата рождения (dotted) = {}", result.get());
                return result;
            }
        }
        // 2) Гибкий путь — покрывает СТД-Р («23 » 09 1977) и «рваные» цифры.
        Matcher flex = P_BIRTH_DATE_FLEXIBLE.matcher(text);
        if (flex.find()) {
            String dayRaw   = flex.group(1);
            String monthRaw = flex.group(2);
            String yearRaw  = flex.group(3);
            try {
                int day   = Integer.parseInt(dayRaw.replaceAll("\\s+", ""));
                int month = Integer.parseInt(monthRaw.replaceAll("\\s+", ""));
                int year  = Integer.parseInt(yearRaw.replaceAll("\\s+", ""));
                LocalDate date = LocalDate.of(year, month, day);
                log.debug("PdfFieldParser: дата рождения (flexible) = {} (raw='{}'/'{}'/'{}')",
                        date, dayRaw, monthRaw, yearRaw);
                return Optional.of(date);
            } catch (Exception ex) {
                log.warn("PdfFieldParser: не удалось собрать дату рождения из «{}»/«{}»/«{}»: {}",
                        dayRaw, monthRaw, yearRaw, ex.getMessage());
            }
        }
        log.debug("PdfFieldParser: дата рождения не найдена");
        return Optional.empty();
    }

    /**
     * Извлекает серию и номер паспорта из 2-НДФЛ.
     * @return {@code [series, number]} или пустой Optional
     */
    public Optional<String[]> parsePassport(String text) {
        Matcher m = P_PASSPORT.matcher(text);
        if (!m.find()) {
            log.debug("PdfFieldParser: серия/номер паспорта не найдены");
            return Optional.empty();
        }
        String series = m.group(1).replaceAll("\\s+", "");  // «77 01» → «7701»
        String number = m.group(2);
        log.debug("PdfFieldParser: распознан паспорт = {} {}", series, number);
        return Optional.of(new String[]{series, number});
    }

    /**
     * Возвращает 10-значный ИНН организации (работодателя). В шапке
     * СТД-Р/2-НДФЛ он идёт после заголовка организации; в 2-НДФЛ
     * параллельно может встретиться 12-значный ИНН физлица, но мы
     * используем этот метод только для СТД-Р, где такого ИНН нет.
     */
    public Optional<String> parseEmployerInn(String text) {
        Matcher m = P_INN_ORGANIZATION.matcher(text);
        if (m.find()) {
            String inn = m.group(1);
            log.debug("PdfFieldParser: найден ИНН работодателя = {}", inn);
            return Optional.of(inn);
        }
        return Optional.empty();
    }

    /**
     * Извлекает помесячные суммы дохода из таблицы 2-НДФЛ и возвращает
     * среднюю ежемесячную зарплату.
     *
     * <p>Алгоритм:
     * <ol>
     *   <li>находим все строки таблицы по {@link #P_INCOME_ROW};</li>
     *   <li>группируем суммы по уникальному номеру месяца (в одном
     *       месяце может быть несколько строк с разными кодами дохода —
     *       зарплата 2000, отпускные 2012 и т.п.);</li>
     *   <li>делим итог на количество <b>уникальных месяцев</b>, а не на
     *       число строк (иначе средняя сильно занижается).</li>
     * </ol>
     *
     * <p>Возвращает пустой Optional, если таблица не найдена или не
     * распарсилась.
     */
    public Optional<BigDecimal> parseAverageMonthlyIncome(String text) {
        Matcher m = P_INCOME_ROW.matcher(text);
        Map<String, BigDecimal> byMonth = new TreeMap<>();
        while (m.find()) {
            String month = m.group(1);
            BigDecimal amount = parseMoney(m.group(3));
            if (amount != null) {
                byMonth.merge(month, amount, BigDecimal::add);
            }
        }
        if (byMonth.isEmpty()) {
            log.debug("PdfFieldParser: таблица доходов не найдена");
            return Optional.empty();
        }
        BigDecimal sum = byMonth.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(byMonth.size()), 2, RoundingMode.HALF_UP);
        log.debug("PdfFieldParser: уникальных месяцев = {}, общая сумма = {}, среднемесячная = {}",
                byMonth.size(), sum, avg);
        return Optional.of(avg);
    }

    /**
     * Парсит все записи о трудовой деятельности из СТД-Р в порядке
     * появления. На уровне сервиса валидации эта последовательность
     * используется, чтобы определить:
     * <ol>
     *     <li>уволен ли сотрудник в настоящий момент (последняя по
     *     дате запись — УВОЛЬНЕНИЕ);</li>
     *     <li>дату <b>текущего</b> трудоустройства — последний ПРИЕМ,
     *     если после него не было увольнения;</li>
     *     <li>стаж в месяцах как разность между датой ПРИЕМа и текущей
     *     датой (см. {@code DocumentValidationService}).</li>
     * </ol>
     */
    public List<EmploymentRecord> parseEmploymentRecords(String text) {
        Matcher m = P_EMPLOYMENT_RECORD.matcher(text);
        List<EmploymentRecord> records = new ArrayList<>();
        while (m.find()) {
            String dateStr = m.group(1);
            String action  = m.group(2).toUpperCase(Locale.ROOT);
            tryParseDottedDate(dateStr).ifPresent(date ->
                    records.add(new EmploymentRecord().setDate(date).setAction(normalizeAction(action))));
        }
        log.debug("PdfFieldParser: найдено {} записей трудовой деятельности", records.size());
        return records;
    }

    // ── Вспомогательные методы ───────────────────────────────────────────────

    private Optional<String> findFirstGroup(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? Optional.of(m.group(1).trim()) : Optional.empty();
    }

    private Optional<LocalDate> tryParseDottedDate(String s) {
        try {
            return Optional.of(LocalDate.parse(s, DATE_DOTTED));
        } catch (Exception ex) {
            log.warn("PdfFieldParser: не удалось распарсить дату '{}': {}", s, ex.getMessage());
            return Optional.empty();
        }
    }

    /** «36 000,00» / «36000.00» → BigDecimal(36000.00). */
    private BigDecimal parseMoney(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("\\s+", "").replace(',', '.');
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            log.warn("PdfFieldParser: некорректная сумма '{}'", raw);
            return null;
        }
    }

    private String normalizeAction(String action) {
        // ПРИЁМ и ПРИЕМ — одно и то же действие
        return "ПРИЁМ".equals(action) ? "ПРИЕМ" : action;
    }

    // ── Внутренние модели ────────────────────────────────────────────────────

    /** Одна запись о трудовой деятельности из СТД-Р. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class EmploymentRecord {
        LocalDate date;
        /** {@code ПРИЕМ} / {@code ПЕРЕВОД} / {@code УВОЛЬНЕНИЕ}. */
        String action;
    }
}
