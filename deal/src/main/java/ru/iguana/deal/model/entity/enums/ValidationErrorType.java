package ru.iguana.deal.model.entity.enums;

/**
 * Тип ошибки, возникшей в процессе автоматической валидации документов.
 *
 * <p>Делится на три категории:
 * <ul>
 *     <li><b>Несовпадения полей</b> — значение в документе и в заявке отличаются
 *     (FIO_MISMATCH, BIRTH_DATE_MISMATCH, PASSPORT_MISMATCH,
 *     EMPLOYER_INN_MISMATCH, SALARY_MISMATCH, WORK_EXPERIENCE_MISMATCH).</li>
 *     <li><b>Структурные ошибки</b> — поля не удалось извлечь или документы
 *     отсутствуют (FIELD_NOT_FOUND, DOCUMENT_MISSING, PDF_PARSE_ERROR).</li>
 *     <li><b>Бизнес-правила</b> — в выписке СТД-Р последняя операция —
 *     УВОЛЬНЕНИЕ, значит клиент не трудоустроен (UNEMPLOYED).</li>
 * </ul>
 */
public enum ValidationErrorType {

    // ── Несовпадения значений ────────────────────────────────────────────────
    FIO_MISMATCH,
    BIRTH_DATE_MISMATCH,
    PASSPORT_MISMATCH,
    EMPLOYER_INN_MISMATCH,
    SALARY_MISMATCH,
    WORK_EXPERIENCE_MISMATCH,

    // ── Структурные ошибки ───────────────────────────────────────────────────
    /** Поле найдено в заявке, но не обнаружено в PDF-документе. */
    FIELD_NOT_FOUND,
    /** Один из обязательных документов (2-НДФЛ или СТД-Р) не загружен. */
    DOCUMENT_MISSING,
    /** PDF-файл повреждён или не удалось его распарсить. */
    PDF_PARSE_ERROR,

    // ── Бизнес-правила ───────────────────────────────────────────────────────
    /** По данным СТД-Р клиент в настоящий момент не трудоустроен. */
    UNEMPLOYED,

    /** Любая прочая непредвиденная ошибка валидации. */
    INTERNAL_ERROR
}
