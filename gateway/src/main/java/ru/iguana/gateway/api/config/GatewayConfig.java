package ru.iguana.gateway.api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfig {

    @Bean
    @Qualifier("statementRestClient")
    public RestClient statementRestClient(GatewayProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getStatementUrl())
                .build();
    }

    @Bean
    @Qualifier("dealRestClient")
    public RestClient dealRestClient(GatewayProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getDealUrl())
                .build();
    }
    @Bean
    @Qualifier("rolesRestClient")
    public RestClient rolesRestClient(GatewayProperties statementProperties){
        return RestClient.builder().baseUrl(statementProperties.getRolesUrl()).build();
    }
}
