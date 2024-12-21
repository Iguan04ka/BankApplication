package ru.iguana.statement.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
public class LoanStatementRequestDto {
    @NotNull()
    @DecimalMin(value = "20000.0", message = "Amount must be greater than 20000")
    @Schema(description = "Requested loan amount")
    BigDecimal amount;

    @NotNull()
    @Min(value = 6)
    @Schema(description = "Loan term in months")
    Integer term;

    @NotNull()
    @Size(min = 2, max = 30, message = "First name should not exceed 30 characters")
    @Schema(description = "Borrower's first name")
    String firstName;

    @NotNull()
    @Size(min = 2, max = 30, message = "Last name should not exceed 30 characters")
    @Schema(description = "Borrower's last name")
    String lastName;

    @Size(min = 2, max = 30, message = "Middle name should not exceed 30 characters")
    @Schema(description = "Borrower's middle name")
    String middleName;

    @NotNull()
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be empty")
    @Schema(description = "Borrower's email address")
    String email;

    @NotNull()
    @Past(message = "Birthdate must be in the past")
    @Schema(description = "Borrower's date of birth")
    LocalDate birthdate;

    @NotNull()
    @NotBlank(message = "Passport series cannot be empty")
    @Size(min = 4, max = 4, message = "Employer passport series must be 4 numbers")
    @Pattern(regexp = "^[0-9]+$", message = "Passport series must be 4 numbers")
    @Schema(description = "Passport series of the borrower")
    String passportSeries;

    @NotBlank(message = "Passport series cannot be empty")
    @Size(min = 6, max = 6, message = "Employer passport number must be 6 numbers")
    @Pattern(regexp = "^[0-9]+$", message = "Passport number must be 6 numbers")
    @Schema(description = "Passport number of the borrower")
    @NotNull()
    String passportNumber;
}

