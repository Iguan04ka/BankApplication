package ru.iguana.calculator.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.iguana.calculator.api.enums.Gender;
import ru.iguana.calculator.api.enums.MaritalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class ScoringDataDto {
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
    @Schema(description = "Borrower's gender")
    Gender gender;

    @NotNull()
    @Past(message = "Birthdate must be in the past")
    @Schema(description = "Borrower's date of birth")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate birthdate;

    @NotNull()
    @NotBlank(message = "Passport series cannot be empty")
    @Size(min = 4, max = 4, message = "Employer passport series must be 4 numbers")
    @Pattern(regexp = "^[0-9]+$", message = "Passport series must be 4 numbers")
    @Schema(description = "Passport series of the borrower")
    String passportSeries;

    @NotNull()
    @Size(min = 6, max = 6, message = "Employer passport number must be 6 numbers")
    @Pattern(regexp = "^[0-9]+$", message = "Passport number must be 6 numbers")
    @Schema(description = "Passport number of the borrower")
    @NotNull()
    @NotBlank
    String passportNumber;

    @NotNull()
    @Past(message = "Passport issue date must be in the past")
    @Schema(description = "Date when the passport was issued")
    LocalDate passportIssueDate;

    @NotNull()
    @Size(min = 5, max = 50, message = "Passport issue branch should not exceed 50 characters")
    @Schema(description = "Passport issue branch")
    String passportIssueBranch;

    @NotNull()
    @Schema(description = "Borrower's marital status")
    MaritalStatus maritalStatus;

    @NotNull()
    @Min(value = 0, message = "Dependent amount must be zero or greater")
    @Schema(description = "Number of dependents")
    Integer dependentAmount;

    @NotNull()
    @Schema(description = "Borrower's employment information")
    EmploymentDto employment;

    @NotNull()
    @Pattern(regexp = "^[0-9]{20}$", message = "Account number must be exactly 20 digits")
    @Schema(description = "Borrower's bank account number")
    String accountNumber;

    @NotNull()
    @Schema(description = "Is insurance enabled")
    Boolean isInsuranceEnabled;

    @NotNull()
    @Schema(description = "Is the borrower a salary client")
    Boolean isSalaryClient;
}
