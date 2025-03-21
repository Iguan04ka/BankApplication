package ru.iguana.calculator.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class OfferParameters {
     Boolean isInsuranceEnabled;
     Boolean isSalaryClient;
}
