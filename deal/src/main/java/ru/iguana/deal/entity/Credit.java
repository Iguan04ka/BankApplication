package ru.iguana.deal.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(schema = "public", name = "credit")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Credit {

    @Id
    @GeneratedValue()
    @Column(name = "credit_id", columnDefinition = "UUID")
    UUID creditId;

    @Column(name = "amount")
    BigDecimal amount;

    @Column(name = "term")
    Integer term;

    @Column(name = "monthly_payment")
    BigDecimal monthlyPayment;

    @Column(name = "rate")
    BigDecimal rate;

    @Column(name = "psk")
    BigDecimal psk;

    @Column(name = "payment_schedule")
    String paymentSchedule;

    @Column(name = "insurance_enabled")
    Boolean isInsuranceEnabled;

    @Column(name = "salary_client")
    Boolean isSalaryClient;

    @Column(name = "credit_status")
    String creditStatus;
}
