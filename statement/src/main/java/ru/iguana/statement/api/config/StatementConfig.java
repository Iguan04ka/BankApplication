package ru.iguana.statement.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class StatementConfig {

    @Bean
    public WebClient webClient(StatementProperties statementProperties){
        return WebClient.builder().baseUrl(statementProperties.getBaseUrl()).build();
    }
}
