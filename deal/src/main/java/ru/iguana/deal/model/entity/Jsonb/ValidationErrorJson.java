package ru.iguana.deal.model.entity.Jsonb;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.model.entity.enums.ValidationErrorType;

/**
 * Одна ошибка автоматической валидации документов. Сохраняется в составе
 * массива {@code errors} в таблице {@code document_validation_result} как
 * элемент JSONB.
 *
 * <p>{@code expected} и {@code actual} приводятся к строкам, чтобы корректно
 * сравнивать разные типы (даты, числа, ИНН, ФИО) в одном формате.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationErrorJson {

    /** Имя проверяемого поля заявки (например, {@code "employmentDuration"}). */
    String field;

    /** Ожидаемое значение — то, что указал пользователь в заявке. */
    String expected;

    /** Фактическое значение — то, что извлечено из PDF-документа. */
    String actual;

    /** Человекочитаемое сообщение об ошибке. */
    String message;

    /** Категория ошибки — позволяет фронту/админке группировать проблемы. */
    ValidationErrorType errorType;
}
