package ru.iguana.deal.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;
@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditDto {
    UUID creditId;

    BigDecimal amount;

    Integer term;

    BigDecimal monthlyPayment;

    BigDecimal rate;

    BigDecimal psk;

    String paymentSchedule;

    Boolean isInsuranceEnabled;

    Boolean isSalaryClient;

    String creditStatus;
}
