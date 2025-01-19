package ru.iguana.gateway.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@ConfigurationProperties(prefix = "microservice")
@Component
@Getter
@Setter
public class GatewayProperties {
    private String dealUrl;
    private String statementUrl;
}
