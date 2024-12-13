package ru.iguana.deal.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Registration data required for credit calculation")
public class FinishRegistrationRequestDto {
    @Schema(description = "Gender of the applicant", example = "MALE")
    String gender;

    @Schema(description = "Marital status of the applicant", example = "MARRIED")
    String maritalStatus;

    @Schema(description = "Number of dependents", example = "2")
    Integer dependentAmount;

    @Schema(description = "Passport issue date", example = "2020-01-01")
    LocalDate passportIssueDate;

    @Schema(description = "Passport issuing branch", example = "Department 123")
    String passportIssueBranch;

    @Schema(description = "EmploymentDto")
    JsonNode employment;

    @Schema(description = "Account number", example = "1234567890")
    String accountNumber;
}
