package ru.iguana.deal.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;
import ru.iguana.deal.entity.Jsonb.StatusHistory;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(schema = "public", name = "statement")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Statement {

    @Id
    @GeneratedValue()
    @Column(name = "statement_id", columnDefinition = "UUID")
    UUID statementId;

    @Column(name = "client_id", columnDefinition = "UUID")
    UUID clientId;

    @Column(name = "credit_id", columnDefinition = "UUID")
    UUID credit;

    @Column(name = "status")
    String status;

    @Column(name = "creation_date")
    Timestamp creationDate;

    @Type(JsonType.class)
    @Column(name = "applied_offer", columnDefinition = "jsonb")
    String appliedOffer;

    @Column(name = "sign_date")
    Timestamp signDate;

    @Column(name = "ses_code")
    String sesCode;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", name = "status_history")
    StatusHistory statusHistory;
}
