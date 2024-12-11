package ru.iguana.deal.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.entity.Jsonb.StatusHistory;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatementDto {
    UUID statementId;

    UUID clientId;

    UUID credit;

    String status;

    Timestamp creationDate;

    String appliedOffer;

    Timestamp signDate;

    String sesCode;

    StatusHistory statusHistory;
}

