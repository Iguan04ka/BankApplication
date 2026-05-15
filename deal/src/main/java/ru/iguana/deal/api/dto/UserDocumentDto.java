package ru.iguana.deal.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Метаданные пользовательского документа (без бинарного содержимого).
 */
@Data
@Accessors(chain = true)
public class UserDocumentDto {

    UUID documentId;
    UUID clientId;
    String documentType;
    String originalFileName;
    String mimeType;
    Long fileSize;
    Timestamp uploadedAt;
}
