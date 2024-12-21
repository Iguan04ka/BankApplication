package ru.iguana.deal.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "microservice")
@Getter
@Setter
public class DealProperties {
    private String baseUrl;
}
