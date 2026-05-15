package ru.iguana.deal.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;
import ru.iguana.deal.model.entity.Jsonb.Employment;
import ru.iguana.deal.model.entity.Jsonb.Passport;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "public", name = "client")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
public class Client {

    @Id
    @GeneratedValue()
    @Column(name = "client_id", columnDefinition = "UUID")
    UUID clientId;

    // Все поля, кроме client_id, — явно nullable = true, чтобы исключить
    // создание NOT NULL DDL-ограничений при ddl-auto: update на свежей БД.
    // Клиент создаётся при регистрации с минимальным набором полей
    // (client_id, user_sub, email); остальные заполняются при оформлении заявки.

    @Column(name = "last_name", nullable = true)
    String lastName;

    @Column(name = "first_name", nullable = true)
    String firstName;

    @Column(name = "middle_name", nullable = true)
    String middleName;

    @Column(name = "user_sub", nullable = true)
    String userSub;

    @Column(name = "birth_date", nullable = true)
    LocalDate birthDate;

    @Column(name = "email", nullable = true)
    String email;

    @Column(name = "gender", nullable = true)
    String gender;

    @Column(name = "marital_status", nullable = true)
    String maritalStatus;

    @Column(name = "dependent_amount", nullable = true)
    Integer dependentAmount;

    @Type(JsonType.class)
    @Column(name = "passport", columnDefinition = "jsonb", nullable = true)
    Passport passport;

    @Type(JsonType.class)
    @Column(name = "employment", columnDefinition = "jsonb", nullable = true)
    Employment employment;

    @Column(name = "account_number", nullable = true)
    String accountNumber;

}
