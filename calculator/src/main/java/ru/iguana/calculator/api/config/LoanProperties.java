package ru.iguana.calculator.api.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ConfigurationProperties(prefix = "loan")
public class LoanProperties {
    BigDecimal baseRate;

    BigDecimal baseCostOfInsurance;
}
