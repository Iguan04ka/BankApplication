package ru.iguana.deal.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import ru.iguana.deal.model.entity.Jsonb.Employment;
import ru.iguana.deal.model.entity.Jsonb.Passport;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientUpdateRequestDto {
    String lastName;
    String firstName;
    String middleName;

    /**
     * Дата рождения клиента. Принимается в формате ISO {@code YYYY-MM-DD}
     * (фронт отправляет значение HTML-инпута {@code type="date"} как есть).
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate birthDate;

    String email;
    String gender;
    String maritalStatus;
    Integer dependentAmount;
    Passport passport;
    Employment employment;
    String accountNumber;
}
