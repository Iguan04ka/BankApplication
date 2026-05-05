package ru.iguana.deal.api.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Summary projection of a statement for the admin list view.
 * Includes the client's name + the credit's amount/term so the table
 * can render without N+1 round-trips on the frontend.
 */
@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminStatementSummaryDto {
    UUID statementId;
    UUID clientId;
    UUID creditId;
    String status;
    Timestamp creationDate;
    BigDecimal requestedAmount;
    Integer requestedTerm;

    // optional joined fields
    String clientFullName;
    String clientUserSub;
    String clientEmail;
    BigDecimal creditAmount;
    Integer creditTerm;
    BigDecimal creditRate;
    BigDecimal creditMonthlyPayment;
    String creditStatus;
}
