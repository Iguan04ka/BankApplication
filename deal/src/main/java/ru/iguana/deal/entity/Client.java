package ru.iguana.deal.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;
import ru.iguana.deal.entity.Jsonb.Employment;
import ru.iguana.deal.entity.Jsonb.Passport;

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

    @Column(name = "last_name")
    String lastName;

    @Column(name = "first_name")
    String firstName;

    @Column(name = "middle_name")
    String middleName;

    @Column(name = "birth_date")
    LocalDate birthDate;

    @Column(name = "email")
    String email;

    @Column(name = "gender")
    String gender;

    @Column(name = "marital_status")
    String maritalStatus;

    @Column(name = "dependent_amount")
    Integer dependentAmount;

    @Type(JsonType.class)
    @Column(name = "passport", columnDefinition = "jsonb")
    Passport passport;

    @Type(JsonType.class)
    @Column(name = "employment", columnDefinition = "jsonb")
    Employment employment;

    @Column(name = "account_number")
    String accountNumber;

}
