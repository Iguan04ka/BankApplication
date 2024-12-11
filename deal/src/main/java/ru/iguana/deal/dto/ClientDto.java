package ru.iguana.deal.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.entity.Jsonb.Employment;
import ru.iguana.deal.entity.Jsonb.Passport;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientDto {
    String lastName;

    String firstName;

    String middleName;

    LocalDate birthDate;

    String email;

    String gender;

    String maritalStatus;

    Integer dependentAmount;

    Passport passport;

    Employment employment;

    String accountNumber;
}
