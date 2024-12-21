package ru.iguana.deal.model.entity.Jsonb;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Employment {
    String status;

    String employer_inn;

    BigDecimal salary;

    String position;

    Integer workExperienceTotal;

    Integer workExperienceCurrent;
}
