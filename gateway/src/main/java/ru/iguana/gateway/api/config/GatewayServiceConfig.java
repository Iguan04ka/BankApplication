package ru.iguana.gateway.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayServiceConfig {
    @Bean
    public WebClient webClient(GatewayProperties gatewayProperties){
        return WebClient.builder().baseUrl(gatewayProperties.getBaseUrl()).build();
    }
}
