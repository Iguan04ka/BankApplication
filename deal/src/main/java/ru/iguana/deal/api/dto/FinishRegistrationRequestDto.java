package ru.iguana.deal.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FinishRegistrationRequestDto {
    String gender;

    String maritalStatus;

    Integer dependentAmount;

    LocalDate passportIssueDate;

    String passportIssueBranch;

    JsonNode employment;

    String accountNumber;

}
