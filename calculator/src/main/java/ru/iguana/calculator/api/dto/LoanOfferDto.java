package ru.iguana.calculator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class LoanOfferDto {
    @NotNull()
    @Schema(description = "Unique identifier for the loan offer statement")
    UUID statementId;

    @NotNull()
    @DecimalMin(value = "0.01", message = "Requested amount must be greater than 0")
    @Schema(description = "Requested loan amount")
    BigDecimal requestedAmount;

    @NotNull()
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Schema(description = "Total loan amount (including fees, insurance, etc.)")
    BigDecimal totalAmount;

    @NotNull()
    @Min(value = 6)
    @Schema(description = "Loan term in months")
    Integer term;

    @NotNull()
    @DecimalMin(value = "0.01", message = "Monthly payment must be greater than 0")
    @Schema(description = "Monthly payment for the loan")
    BigDecimal monthlyPayment;

    @NotNull()
    @DecimalMin(value = "0.0", message = "Rate must be greater than or equal to 0")
    @DecimalMin(value = "100.0", message = "Rate must be less than or equal to 100")
    @Schema(description = "Annual interest rate for the loan")
    BigDecimal rate;

    @NotNull()
    @Schema(description = "Indicates if insurance is enabled")
    Boolean isInsuranceEnabled;

    @NotNull()
    @Schema(description = "Indicates if the client is a salary client")
    Boolean isSalaryClient;
}
