package ru.iguana.calculator.api.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class LoanProperties {
    @Value("${loan.baseRate}")
    BigDecimal baseRate;
}
