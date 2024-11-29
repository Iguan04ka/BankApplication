package ru.iguana.calculator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class PaymentScheduleElementDto {
    @NotNull()
    @Min(value = 1, message = "Number must be greater than or equal to 1")
    @Schema(description = "Payment number in the schedule")
    Integer number;

    @NotNull()
    @FutureOrPresent(message = "Date must be in the future or present")
    @Schema(description = "Payment date")
    LocalDate date;

    @NotNull()
    @DecimalMin(value = "0.01", message = "Total payment must be greater than 0")
    @Schema(description = "Total payment amount")
    BigDecimal totalPayment;

    @DecimalMin(value = "0.01", message = "Interest payment must be greater than 0")
    @Schema(description = "Interest payment amount")
    @NotNull()
    BigDecimal interestPayment;

    @NotNull()
    @DecimalMin(value = "0.01", message = "Debt payment must be greater than 0")
    @Schema(description = "Debt payment amount")
    BigDecimal debtPayment;

    @NotNull()
    @DecimalMin(value = "0.00", message = "Remaining debt must be greater than or equal to 0")
    @Schema(description = "Remaining debt after the payment")
    BigDecimal remainingDebt;
}
