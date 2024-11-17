package ru.iguana.calculator.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.iguana.calculator.api.enums.EmploymentStatus;
import ru.iguana.calculator.api.enums.Positions;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class EmploymentDto {
    EmploymentStatus employmentStatus;
    String employerINN;
    BigDecimal salary;
    Positions position;
    Integer workExperienceTotal;
    Integer workExperienceCurrent;
}
