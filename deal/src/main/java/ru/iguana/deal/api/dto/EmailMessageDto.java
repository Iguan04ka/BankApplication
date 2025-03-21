package ru.iguana.deal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.model.entity.enums.EmailTheme;

import java.util.UUID;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "DTO for email message details")
public class EmailMessageDto {
    @Schema(description = "Recipient email address", example = "user@example.com")
    String address;

    @Schema(description = "Email theme", example = "SEND_DOCUMENTS")
    EmailTheme theme;

    @Schema(description = "Unique statement ID", example = "6bacc7ba-5568-4566-b0a5-8e324462b763")
    UUID statementId;

    @Schema(description = "Email message content", example = "Please sign the documents")
    String text;
}
