package ru.iguana.deal.api.service.validation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.iguana.deal.api.dto.ValidationErrorDto;
import ru.iguana.deal.api.dto.ValidationResultDto;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.DocumentValidationResult;
import ru.iguana.deal.model.entity.Jsonb.Employment;
import ru.iguana.deal.model.entity.Jsonb.Passport;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Jsonb.ValidationErrorJson;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.UserDocument;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.entity.enums.UserDocumentType;
import ru.iguana.deal.model.entity.enums.ValidationErrorType;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.DocumentValidationResultRepository;
import ru.iguana.deal.model.repository.StatementRepository;
import ru.iguana.deal.model.repository.UserDocumentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Координирует автоматическую валидацию PDF-документов по одной заявке.
 *
 * <p>Алгоритм:
 * <ol>
 *     <li>Подгружаются заявка, клиент и оба обязательных документа
 *     (2-НДФЛ + СТД-Р). Отсутствие любого из них → ошибка
 *     {@link ValidationErrorType#DOCUMENT_MISSING}.</li>
 *     <li>Из PDF извлекается текст ({@link PdfTextExtractor}) и
 *     разбираются интересующие поля ({@link PdfFieldParser}).</li>
 *     <li>Каждое поле сравнивается с соответствующим значением заявки;
 *     несовпадения собираются в список {@link ValidationErrorJson}.</li>
 *     <li>Если список пустой → заявка переводится в {@code CREDIT_ISSUED};
 *     иначе остаётся в {@code DOCUMENT_SIGNED} и ждёт менеджера.</li>
 *     <li>Каждая попытка валидации сохраняется в
 *     {@code document_validation_result} как отдельная запись истории.</li>
 * </ol>
 *
 * <p>Метод {@link #validateAndApply(UUID)} безопасен к повторному запуску:
 * успешная валидация может быть выполнена несколько раз, что переводит
 * заявку из {@code DOCUMENT_SIGNED} в {@code CREDIT_ISSUED}. Если заявка
 * уже в {@code CREDIT_ISSUED}, статус не понижается обратно даже при
 * неуспешной валидации — только формируется новая запись истории.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentValidationService {

    private final StatementRepository statementRepository;
    private final ClientRepository clientRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final DocumentValidationResultRepository validationResultRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final PdfFieldParser pdfFieldParser;

    /** Допустимое отклонение средней зарплаты от значения заявки, %. */
    @Value("${validation.salary.tolerance-percent:15}")
    private int salaryTolerancePercent;

    /** Допустимое отклонение текущего стажа от значения заявки, месяцев. */
    @Value("${validation.work-experience.tolerance-months:2}")
    private int workExperienceToleranceMonths;

    // ── Точка входа ──────────────────────────────────────────────────────────

    /**
     * Запускает валидацию, применяет её результат к статусу заявки и
     * возвращает DTO для ответа клиенту.
     *
     * <p>Транзакционен — все изменения статуса и записи истории идут
     * одной БД-транзакцией.
     */
    @Transactional
    public ValidationResultDto validateAndApply(UUID statementId) {
        log.info("DocumentValidation: запуск валидации для statementId={}", statementId);

        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> {
                    log.warn("DocumentValidation: заявка не найдена, id={}", statementId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found");
                });

        Client client = clientRepository.findById(statement.getClientId())
                .orElseThrow(() -> {
                    log.warn("DocumentValidation: клиент не найден для statementId={}", statementId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
                });

        List<ValidationErrorJson> errors = runChecks(statement, client);

        // Решаем итоговый статус. CREDIT_ISSUED не «откатываем» обратно
        // при повторных запусках с ошибками — только история обновляется.
        ApplicationStatus alreadyIssued = ApplicationStatus.CREDIT_ISSUED.name().equals(statement.getStatus())
                ? ApplicationStatus.CREDIT_ISSUED
                : null;

        ApplicationStatus newStatus;
        if (errors.isEmpty()) {
            newStatus = ApplicationStatus.CREDIT_ISSUED;
            log.info("DocumentValidation: проверка успешна, заявка {} → CREDIT_ISSUED", statementId);
        } else if (alreadyIssued != null) {
            newStatus = ApplicationStatus.CREDIT_ISSUED;
            log.warn("DocumentValidation: проверка с ошибками ({}), но заявка уже CREDIT_ISSUED — статус сохранён",
                    errors.size());
        } else {
            newStatus = ApplicationStatus.DOCUMENT_SIGNED;
            log.warn("DocumentValidation: проверка не пройдена для {}, ошибок = {}, статус остаётся DOCUMENT_SIGNED",
                    statementId, errors.size());
            for (ValidationErrorJson err : errors) {
                log.warn("  → {}: {} (ожидалось={}, фактически={})",
                        err.getErrorType(), err.getMessage(), err.getExpected(), err.getActual());
            }
        }

        applyStatus(statement, newStatus);

        DocumentValidationResult saved = persistResult(statement, client, errors, newStatus);
        return toDto(saved);
    }

    /** Возвращает последний сохранённый результат валидации без перезапуска. */
    public Optional<ValidationResultDto> getLastResult(UUID statementId) {
        return validationResultRepository.findFirstByStatementIdOrderByValidatedAtDesc(statementId)
                .map(this::toDto);
    }

    /** Полная история попыток валидации, новые сверху. */
    public List<ValidationResultDto> getHistory(UUID statementId) {
        return validationResultRepository.findAllByStatementIdOrderByValidatedAtDesc(statementId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ── Список проверок ──────────────────────────────────────────────────────

    private List<ValidationErrorJson> runChecks(Statement statement, Client client) {
        List<ValidationErrorJson> errors = new ArrayList<>();

        Optional<UserDocument> ndflOpt = userDocumentRepository
                .findByClientIdAndDocumentType(client.getClientId(), UserDocumentType.NDFL_2.name());
        Optional<UserDocument> stdrOpt = userDocumentRepository
                .findByClientIdAndDocumentType(client.getClientId(), UserDocumentType.EMPLOYMENT_RECORD.name());

        if (ndflOpt.isEmpty()) {
            errors.add(error("documents.NDFL_2", null, null,
                    "Не загружена справка 2-НДФЛ", ValidationErrorType.DOCUMENT_MISSING));
        }
        if (stdrOpt.isEmpty()) {
            errors.add(error("documents.EMPLOYMENT_RECORD", null, null,
                    "Не загружена выписка СТД-Р", ValidationErrorType.DOCUMENT_MISSING));
        }
        // Если хотя бы один из документов отсутствует, не пытаемся парсить — раннее
        // возвращение делает логи чище и не порождает каскад FIELD_NOT_FOUND.
        if (!errors.isEmpty()) {
            return errors;
        }

        String ndflText = safeExtract(ndflOpt.get(), errors, "NDFL_2");
        String stdrText = safeExtract(stdrOpt.get(), errors, "EMPLOYMENT_RECORD");
        if (ndflText == null || stdrText == null) {
            return errors;
        }
        ndflText = pdfFieldParser.normalize(ndflText);
        stdrText = pdfFieldParser.normalize(stdrText);

        // Проверки — каждая независимо логирует свой результат.
        checkFio(client, ndflText, stdrText, errors);
        checkBirthDate(client, ndflText, stdrText, errors);
        checkPassport(client, ndflText, errors);
        checkEmployerInn(client, stdrText, errors);
        checkSalary(client, ndflText, errors);
        checkEmploymentDuration(client, stdrText, errors);

        return errors;
    }

    // ── Конкретные проверки ──────────────────────────────────────────────────

    private void checkFio(Client client, String ndflText, String stdrText, List<ValidationErrorJson> errors) {
        // Сравниваем по каждому документу отдельно — это даёт точное
        // указание, где именно расхождение.
        compareName("lastName",   client.getLastName(),   pdfFieldParser.parseLastName(ndflText),   "NDFL_2",            errors);
        compareName("firstName",  client.getFirstName(),  pdfFieldParser.parseFirstName(ndflText),  "NDFL_2",            errors);
        compareName("middleName", client.getMiddleName(), pdfFieldParser.parseMiddleName(ndflText), "NDFL_2",            errors);
        compareName("lastName",   client.getLastName(),   pdfFieldParser.parseLastName(stdrText),   "EMPLOYMENT_RECORD", errors);
        compareName("firstName",  client.getFirstName(),  pdfFieldParser.parseFirstName(stdrText),  "EMPLOYMENT_RECORD", errors);
        compareName("middleName", client.getMiddleName(), pdfFieldParser.parseMiddleName(stdrText), "EMPLOYMENT_RECORD", errors);
    }

    private void compareName(String field, String expected, Optional<String> actual,
                             String documentTag, List<ValidationErrorJson> errors) {
        String fieldFq = documentTag + "." + field;
        if (expected == null || expected.isBlank()) {
            // Отчества может не быть — это нормальный кейс, не ошибка.
            return;
        }
        if (actual.isEmpty()) {
            log.debug("checkFio: поле {} не найдено в документе", fieldFq);
            errors.add(error(fieldFq, expected, null,
                    "Поле «" + field + "» не найдено в " + documentTag,
                    ValidationErrorType.FIELD_NOT_FOUND));
            return;
        }
        if (!normalizeName(expected).equals(normalizeName(actual.get()))) {
            errors.add(error(fieldFq, expected, actual.get(),
                    "ФИО в " + documentTag + " не совпадает с данными заявки",
                    ValidationErrorType.FIO_MISMATCH));
        }
    }

    private void checkBirthDate(Client client, String ndflText, String stdrText,
                                List<ValidationErrorJson> errors) {
        LocalDate expected = client.getBirthDate();
        if (expected == null) {
            log.debug("checkBirthDate: дата рождения отсутствует в заявке — проверка пропущена");
            return;
        }
        compareDate("birthDate", expected, pdfFieldParser.parseBirthDate(ndflText), "NDFL_2", errors);
        compareDate("birthDate", expected, pdfFieldParser.parseBirthDate(stdrText), "EMPLOYMENT_RECORD", errors);
    }

    private void compareDate(String field, LocalDate expected, Optional<LocalDate> actual,
                             String documentTag, List<ValidationErrorJson> errors) {
        String fieldFq = documentTag + "." + field;
        if (actual.isEmpty()) {
            errors.add(error(fieldFq, expected.toString(), null,
                    "Дата не найдена в " + documentTag, ValidationErrorType.FIELD_NOT_FOUND));
            return;
        }
        if (!expected.equals(actual.get())) {
            errors.add(error(fieldFq, expected.toString(), actual.get().toString(),
                    "Дата рождения в " + documentTag + " не совпадает с данными заявки",
                    ValidationErrorType.BIRTH_DATE_MISMATCH));
        }
    }

    private void checkPassport(Client client, String ndflText, List<ValidationErrorJson> errors) {
        Passport passport = client.getPassport();
        if (passport == null || isBlank(passport.getSeries()) || isBlank(passport.getNumber())) {
            log.debug("checkPassport: данные паспорта отсутствуют в заявке — проверка пропущена");
            return;
        }
        String expectedSeries = passport.getSeries().replaceAll("\\s+", "");
        String expectedNumber = passport.getNumber().replaceAll("\\s+", "");

        Optional<String[]> parsed = pdfFieldParser.parsePassport(ndflText);
        if (parsed.isEmpty()) {
            errors.add(error("passport", expectedSeries + " " + expectedNumber, null,
                    "Серия и номер паспорта не найдены в 2-НДФЛ",
                    ValidationErrorType.FIELD_NOT_FOUND));
            return;
        }
        String actualSeries = parsed.get()[0];
        String actualNumber = parsed.get()[1];
        boolean seriesOk = expectedSeries.equals(actualSeries);
        boolean numberOk = expectedNumber.equals(actualNumber);
        if (!seriesOk || !numberOk) {
            errors.add(error("passport",
                    expectedSeries + " " + expectedNumber,
                    actualSeries + " " + actualNumber,
                    "Серия/номер паспорта в 2-НДФЛ не совпадают с данными заявки",
                    ValidationErrorType.PASSPORT_MISMATCH));
        }
    }

    private void checkEmployerInn(Client client, String stdrText, List<ValidationErrorJson> errors) {
        Employment employment = client.getEmployment();
        if (employment == null || isBlank(employment.getEmployer_inn())) {
            log.debug("checkEmployerInn: ИНН работодателя отсутствует в заявке — проверка пропущена");
            return;
        }
        String expected = employment.getEmployer_inn().replaceAll("\\s+", "");
        Optional<String> actual = pdfFieldParser.parseEmployerInn(stdrText);
        if (actual.isEmpty()) {
            errors.add(error("employerInn", expected, null,
                    "ИНН работодателя не найден в СТД-Р", ValidationErrorType.FIELD_NOT_FOUND));
            return;
        }
        if (!expected.equals(actual.get())) {
            errors.add(error("employerInn", expected, actual.get(),
                    "ИНН работодателя в СТД-Р не совпадает с данными заявки",
                    ValidationErrorType.EMPLOYER_INN_MISMATCH));
        }
    }

    private void checkSalary(Client client, String ndflText, List<ValidationErrorJson> errors) {
        Employment employment = client.getEmployment();
        if (employment == null || employment.getSalary() == null) {
            log.debug("checkSalary: salary отсутствует в заявке — проверка пропущена");
            return;
        }
        BigDecimal expected = employment.getSalary();
        Optional<BigDecimal> actual = pdfFieldParser.parseAverageMonthlyIncome(ndflText);
        if (actual.isEmpty()) {
            errors.add(error("salary", expected.toPlainString(), null,
                    "Таблица доходов не найдена в 2-НДФЛ",
                    ValidationErrorType.FIELD_NOT_FOUND));
            return;
        }
        BigDecimal actualValue = actual.get();
        BigDecimal diff = actualValue.subtract(expected).abs();
        BigDecimal allowed = expected.multiply(BigDecimal.valueOf(salaryTolerancePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        log.debug("checkSalary: ожидалось={}, фактически={}, |разница|={}, допуск={} ({}%)",
                expected, actualValue, diff, allowed, salaryTolerancePercent);
        if (diff.compareTo(allowed) > 0) {
            errors.add(error("salary", expected.toPlainString(), actualValue.toPlainString(),
                    "Средняя зарплата из 2-НДФЛ отличается от заявленной более чем на "
                            + salaryTolerancePercent + "%",
                    ValidationErrorType.SALARY_MISMATCH));
        }
    }

    private void checkEmploymentDuration(Client client, String stdrText,
                                         List<ValidationErrorJson> errors) {
        Employment employment = client.getEmployment();
        if (employment == null || employment.getWorkExperienceCurrent() == null) {
            log.debug("checkEmploymentDuration: стаж отсутствует в заявке — проверка пропущена");
            return;
        }
        List<PdfFieldParser.EmploymentRecord> records = pdfFieldParser.parseEmploymentRecords(stdrText);
        if (records.isEmpty()) {
            errors.add(error("employmentDuration", String.valueOf(employment.getWorkExperienceCurrent()), null,
                    "Записи о трудовой деятельности не найдены в СТД-Р",
                    ValidationErrorType.FIELD_NOT_FOUND));
            return;
        }

        // Сортируем по дате — чтобы корректно определять «последнее событие».
        records.sort(Comparator.comparing(PdfFieldParser.EmploymentRecord::getDate));
        PdfFieldParser.EmploymentRecord last = records.get(records.size() - 1);

        if ("УВОЛЬНЕНИЕ".equals(last.getAction())) {
            log.warn("checkEmploymentDuration: последняя запись — УВОЛЬНЕНИЕ ({}) — клиент не трудоустроен",
                    last.getDate());
            errors.add(error("employmentDuration",
                    String.valueOf(employment.getWorkExperienceCurrent()), "0",
                    "В СТД-Р последняя запись — УВОЛЬНЕНИЕ; клиент не трудоустроен. "
                            + "Заявка требует ручного рассмотрения менеджером.",
                    ValidationErrorType.UNEMPLOYED));
            return;
        }

        // Ищем дату последнего ПРИЁМа в текущей организации.
        // Алгоритм: идём с конца назад и берём ближайший ПРИЕМ.
        LocalDate hireDate = null;
        for (int i = records.size() - 1; i >= 0; i--) {
            if ("ПРИЕМ".equals(records.get(i).getAction())) {
                hireDate = records.get(i).getDate();
                break;
            }
        }
        if (hireDate == null) {
            errors.add(error("employmentDuration",
                    String.valueOf(employment.getWorkExperienceCurrent()), null,
                    "В СТД-Р не найдена запись о приёме на работу",
                    ValidationErrorType.FIELD_NOT_FOUND));
            return;
        }

        long actualMonths = ChronoUnit.MONTHS.between(hireDate, LocalDate.now());
        int expected = employment.getWorkExperienceCurrent();
        long diff = Math.abs(actualMonths - expected);
        log.debug("checkEmploymentDuration: hireDate={}, ожидалось={} мес, фактически={} мес, |разница|={}, допуск={}",
                hireDate, expected, actualMonths, diff, workExperienceToleranceMonths);
        if (diff > workExperienceToleranceMonths) {
            errors.add(error("employmentDuration", String.valueOf(expected), String.valueOf(actualMonths),
                    "Текущий стаж работы не совпадает с заявленным (допуск ± "
                            + workExperienceToleranceMonths + " мес)",
                    ValidationErrorType.WORK_EXPERIENCE_MISMATCH));
        }
    }

    // ── Сохранение и преобразование ──────────────────────────────────────────

    private DocumentValidationResult persistResult(Statement statement, Client client,
                                                   List<ValidationErrorJson> errors,
                                                   ApplicationStatus finalStatus) {
        DocumentValidationResult result = new DocumentValidationResult()
                .setStatementId(statement.getStatementId())
                .setClientId(client.getClientId())
                .setSuccess(errors.isEmpty())
                .setFinalStatus(finalStatus.name())
                .setErrors(errors);
        return validationResultRepository.save(result);
    }

    private void applyStatus(Statement statement, ApplicationStatus newStatus) {
        if (newStatus.name().equals(statement.getStatus())) {
            return; // статус не меняется — не плодим записи истории
        }
        statement.setStatus(newStatus.name());
        if (statement.getStatusHistory() == null) {
            statement.setStatusHistory(new ArrayList<>());
        }
        statement.getStatusHistory().add(new StatusHistory(
                newStatus, Timestamp.from(java.time.Instant.now()), ChangeType.AUTOMATIC));
        statementRepository.save(statement);
        log.info("DocumentValidation: статус заявки {} обновлён → {}",
                statement.getStatementId(), newStatus);
    }

    private ValidationResultDto toDto(DocumentValidationResult entity) {
        List<ValidationErrorDto> errorDtos = entity.getErrors() == null
                ? List.of()
                : entity.getErrors().stream().map(e -> new ValidationErrorDto()
                        .setField(e.getField())
                        .setExpected(e.getExpected())
                        .setActual(e.getActual())
                        .setMessage(e.getMessage())
                        .setErrorType(e.getErrorType()))
                .toList();
        return new ValidationResultDto()
                .setStatementId(entity.getStatementId())
                .setSuccess(entity.getSuccess())
                .setFinalStatus(entity.getFinalStatus())
                .setValidatedAt(entity.getValidatedAt())
                .setErrors(errorDtos);
    }

    // ── Утилиты ──────────────────────────────────────────────────────────────

    private String safeExtract(UserDocument doc, List<ValidationErrorJson> errors, String tag) {
        try {
            return pdfTextExtractor.extractText(doc.getContent());
        } catch (PdfTextExtractor.PdfExtractionException ex) {
            log.error("DocumentValidation: не удалось извлечь текст из {} (docId={}): {}",
                    tag, doc.getDocumentId(), ex.getMessage());
            errors.add(error("documents." + tag, null, null,
                    "Не удалось извлечь текст из " + tag + ": " + ex.getMessage(),
                    ValidationErrorType.PDF_PARSE_ERROR));
            return null;
        }
    }

    private ValidationErrorJson error(String field, String expected, String actual,
                                      String message, ValidationErrorType type) {
        return new ValidationErrorJson(field, expected, actual, message, type);
    }

    /**
     * Нормализует ФИО для сравнения: приводит к нижнему регистру, удаляет
     * двойные пробелы, обрезает trailing/leading пробелы. Не трогает «ё»/«е» —
     * для имени это значимая разница.
     */
    private String normalizeName(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Перевод java.util.Date → LocalDate (для будущих расширений). */
    @SuppressWarnings("unused")
    private LocalDate toLocalDate(java.util.Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
