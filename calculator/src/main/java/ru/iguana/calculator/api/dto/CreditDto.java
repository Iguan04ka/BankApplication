package ru.iguana.calculator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class CreditDto {

    @NotNull
    @DecimalMin(value = "20000")
    @Schema(description = "Credit amount")
    BigDecimal amount;

    @NotNull()
    @Min(value = 6)
    @Schema(description = "Credit term in months")
    Integer term;

    @NotNull()
    @Schema(description = "Monthly payment")
    BigDecimal monthlyPayment;

    @NotNull()
    @Schema(description = "Interest rate in percentage")
    @DecimalMin(value = "0")
    BigDecimal rate;

    @NotNull()
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Schema(description = "Annual percentage rate (APR) in percentage")
    BigDecimal psk;

    @NotNull()
    @Schema(description = "Flag for insurance")
    Boolean isInsuranceEnabled;

    @NotNull()
    @Schema(description = "Flag for salary client")
    Boolean isSalaryClient;

    @NotNull()
    @Size(min = 1)
    @Schema(description = "Payment schedule")
    List<PaymentScheduleElementDto> paymentSchedule;
}
