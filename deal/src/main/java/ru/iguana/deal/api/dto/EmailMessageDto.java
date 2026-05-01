package ru.iguana.deal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.model.entity.enums.EmailTheme;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "DTO for email message details")
public class EmailMessageDto {
    @Schema(description = "Recipient email address", example = "user@example.com")
    String address;

    @Schema(description = "Email theme", example = "FINISH_REGISTRATION")
    EmailTheme theme;

    @Schema(description = "Unique statement ID", example = "6bacc7ba-5568-4566-b0a5-8e324462b763")
    UUID statementId;

    @Schema(description = "Plain-text fallback / subject hint")
    String text;

    @Schema(description = "Loan amount", example = "500000")
    BigDecimal amount;

    @Schema(description = "Loan term in months", example = "24")
    Integer term;

    @Schema(description = "Interest rate (% per year)", example = "12.5")
    BigDecimal rate;

    @Schema(description = "Monthly payment", example = "23537.89")
    BigDecimal monthlyPayment;

    @Schema(description = "One-time SES confirmation code", example = "493812")
    String code;

    @Schema(description = "Code TTL in minutes", example = "5")
    Integer ttlMinutes;

    @Schema(description = "Recipient first name", example = "Иван")
    String firstName;

    @Schema(description = "Recipient middle name (patronymic)", example = "Иванович")
    String middleName;
}
