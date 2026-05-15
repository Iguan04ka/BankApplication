package ru.iguana.deal.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;
import ru.iguana.deal.model.entity.Jsonb.ValidationErrorJson;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Результат автоматической валидации PDF-документов по одной заявке.
 *
 * <p>Каждый запуск валидации сохраняется как отдельная запись —
 * это позволяет администратору видеть всю историю попыток
 * (например, после повторной загрузки документов и пересборки
 * через {@code POST /deal/admin/documents/{statementId}/validate}).
 *
 * <p>Сам список ошибок хранится в колонке {@code errors} типа JSONB
 * как массив {@link ValidationErrorJson}. {@code finalStatus} фиксирует,
 * в какое состояние была переведена заявка по итогам этой попытки
 * ({@code CREDIT_ISSUED} или {@code DOCUMENT_SIGNED}).
 */
@Entity
@Table(schema = "public", name = "document_validation_result")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
public class DocumentValidationResult {

    @Id
    @GeneratedValue
    @Column(name = "validation_id", columnDefinition = "UUID")
    UUID validationId;

    @Column(name = "statement_id", columnDefinition = "UUID", nullable = false)
    UUID statementId;

    @Column(name = "client_id", columnDefinition = "UUID", nullable = false)
    UUID clientId;

    @Column(name = "validated_at", nullable = false)
    Timestamp validatedAt;

    /** {@code true}, если автоматика прошла без ошибок и кредит выдан. */
    @Column(name = "success", nullable = false)
    Boolean success;

    /** Итоговый статус заявки после применения результата валидации. */
    @Column(name = "final_status", length = 40)
    String finalStatus;

    @Type(JsonType.class)
    @Column(name = "errors", columnDefinition = "jsonb")
    List<ValidationErrorJson> errors = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (this.validatedAt == null) {
            this.validatedAt = Timestamp.from(Instant.now());
        }
    }
}
