package ru.iguana.calculator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.iguana.calculator.api.enums.EmploymentStatus;
import ru.iguana.calculator.api.enums.Positions;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class EmploymentDto {
    @NotNull()
    @Schema(description = "Employment status of the employee")
    EmploymentStatus employmentStatus;

    @NotNull()
    @Size(min = 10, max = 12, message = "Employer INN must be between 10 and 12 characters")
    @Pattern(regexp = "^[0-9]+$", message = "Employer INN must contain only digits")
    @Schema(description = "Employer's INN (tax identification number)")
    String employerINN;

    @NotNull()
    @DecimalMin(value = "0.01", message = "Salary must be greater than 0")
    @Schema(description = "Employee's salary")
    BigDecimal salary;

    @NotNull()
    @Schema(description = "Employee's position")
    Positions position;

    @NotNull()
    @PositiveOrZero(message = "Total work experience must be zero or a positive number")
    @Schema(description = "Total work experience in months")
    Integer workExperienceTotal;

    @PositiveOrZero(message = "Current work experience must be zero or a positive number")
    @Schema(description = "Current work experience with the employer in years")
    @NotNull()
    Integer workExperienceCurrent;
}
