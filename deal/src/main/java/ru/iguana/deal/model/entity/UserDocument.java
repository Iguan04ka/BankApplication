package ru.iguana.deal.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Пользовательский документ (PDF), прикреплённый к заявке на этапе заполнения
 * сведений о трудовой деятельности. Бинарное содержимое файла хранится в
 * том же отношении в колонке {@code content} типа {@code BYTEA}.
 */
@Entity
@Table(schema = "public", name = "user_document",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_document_client_type",
                        columnNames = {"client_id", "document_type"})
        })
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
public class UserDocument {

    @Id
    @GeneratedValue
    @Column(name = "document_id", columnDefinition = "UUID")
    UUID documentId;

    @Column(name = "client_id", columnDefinition = "UUID", nullable = false)
    UUID clientId;

    @Column(name = "document_type", nullable = false, length = 40)
    String documentType;

    @Column(name = "original_file_name", nullable = false, length = 255)
    String originalFileName;

    @Column(name = "mime_type", nullable = false, length = 100)
    String mimeType;

    @Column(name = "file_size", nullable = false)
    Long fileSize;

    @Column(name = "uploaded_at", nullable = false)
    Timestamp uploadedAt;

    /**
     * Бинарное содержимое PDF-файла. Хранится в колонке типа {@code BYTEA};
     * аннотация {@code @Lob} здесь намеренно не используется — иначе
     * PostgreSQL-диалект Hibernate пытается записать значение как
     * Large Object (OID, {@code bigint}), что несовместимо с {@code BYTEA}.
     * В сервисном слое метаданные документа проецируются в DTO без
     * включения этого поля, поэтому подгрузка содержимого происходит
     * только при явном запросе скачивания.
     */
    @Column(name = "content", nullable = false, columnDefinition = "BYTEA")
    byte[] content;

    @PrePersist
    void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = Timestamp.from(Instant.now());
        }
    }
}
