package ru.iguana.deal.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatementDto {
    UUID clientId;

    UUID credit;

    String status;

    Timestamp creationDate;

    JsonNode appliedOffer;

    Timestamp signDate;

    String sesCode;

    List<StatusHistory> statusHistory = new ArrayList<>();
}

