package ru.iguana.deal.api.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDashboardStatsDto {
    long totalStatements;
    long totalCredits;
    BigDecimal totalIssuedAmount;
    /** statementsByStatus: status name → count */
    Map<String, Long> statementsByStatus;
}
