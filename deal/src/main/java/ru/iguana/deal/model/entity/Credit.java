package ru.iguana.deal.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(schema = "public", name = "credit")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
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

    @Type(JsonType.class)
    @Column(name = "payment_schedule", columnDefinition = "jsonb")
    JsonNode paymentSchedule;

    @Column(name = "insurance_enabled")
    Boolean isInsuranceEnabled;

    @Column(name = "salary_client")
    Boolean isSalaryClient;

    @Column(name = "credit_status")
    String creditStatus;
}
