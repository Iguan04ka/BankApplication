package ru.iguana.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Requested loan amount")
    BigDecimal amount;

    @Schema(description = "Loan term in months")
    Integer term;

    @Schema(description = "Borrower's first name")
    String firstName;

    @Schema(description = "Borrower's last name")
    String lastName;

    @Schema(description = "Borrower's middle name")
    String middleName;

    @Schema(description = "Borrower's email address")
    String email;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Borrower's date of birth")
    LocalDate birthdate;

    @Schema(description = "Passport series of the borrower")
    String passportSeries;

    @Schema(description = "Passport number of the borrower")
    String passportNumber;
}

