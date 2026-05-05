package ru.iguana.deal.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Full statement details for admin: the statement itself, the client, and the credit (if any).
 */
@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminStatementDetailDto {
    UUID statementId;
    String status;
    Timestamp creationDate;
    Timestamp signDate;
    BigDecimal requestedAmount;
    Integer requestedTerm;
    JsonNode appliedOffer;
    List<StatusHistory> statusHistory;

    ClientDto client;
    UUID clientId;
    String clientUserSub;

    CreditResponseDto credit;
}
