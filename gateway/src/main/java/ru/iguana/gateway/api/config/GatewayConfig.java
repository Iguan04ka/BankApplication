package ru.iguana.gateway.api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfig {
    @Bean
    @Qualifier("statementWebClient")
    public WebClient statementWebClient(GatewayProperties statementProperties){
        return WebClient.builder().baseUrl(statementProperties.getStatementUrl()).build();
    }
    @Bean
    @Qualifier("dealWebClient")
    public WebClient dealWebClient(GatewayProperties statementProperties){
        return WebClient.builder().baseUrl(statementProperties.getDealUrl()).build();
    }
}
