package ru.iguana.deal.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сводный результат автоматической валидации документов по одной заявке.
 *
 * <p>Возвращается:
 * <ul>
 *     <li>из {@code POST /deal/document/{statementId}/verify} — после
 *     ввода SES-кода; используется фронтом для выбора окна
 *     «Кредит одобрен» / «Заявка передана менеджеру»;</li>
 *     <li>из {@code POST /deal/admin/documents/{statementId}/validate} —
 *     при ручном повторном запуске админом;</li>
 *     <li>из {@code GET /deal/admin/documents/{statementId}/validation-result}
 *     — последний сохранённый результат.</li>
 * </ul>
 *
 * <p>Поле {@code finalStatus} содержит итоговый статус заявки после применения
 * результата: {@code CREDIT_ISSUED} при success=true, {@code DOCUMENT_SIGNED}
 * в остальных случаях.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationResultDto {

    UUID statementId;

    /** {@code true}, если все проверки пройдены успешно. */
    Boolean success;

    /** Итоговый статус заявки после применения результата валидации. */
    String finalStatus;

    /** Момент завершения валидации. */
    Timestamp validatedAt;

    /** Перечень ошибок. Пустой список при {@code success=true}. */
    List<ValidationErrorDto> errors = new ArrayList<>();
}
