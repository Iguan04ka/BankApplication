package ru.iguana.deal.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.model.entity.enums.ValidationErrorType;

/**
 * DTO одной ошибки автоматической валидации, возвращаемой клиенту в API.
 * Зеркалит {@link ru.iguana.deal.model.entity.Jsonb.ValidationErrorJson},
 * но изолирован от JPA-слоя — это публичный контракт.
 *
 * <p>Пример сериализации:
 * <pre>{@code
 * {
 *   "field": "employmentDuration",
 *   "expected": "100",
 *   "actual": "90",
 *   "message": "Стаж работы не совпадает",
 *   "errorType": "WORK_EXPERIENCE_MISMATCH"
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationErrorDto {
    String field;
    String expected;
    String actual;
    String message;
    ValidationErrorType errorType;
}
