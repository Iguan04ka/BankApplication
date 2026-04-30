package ru.iguana.deal.api.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatementShortDto {
    UUID statementId;
    String status;
    Timestamp creationDate;
    BigDecimal requestedAmount;
    Integer term;
}
