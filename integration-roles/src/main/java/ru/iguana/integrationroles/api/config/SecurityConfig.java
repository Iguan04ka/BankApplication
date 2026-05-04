package ru.iguana.integrationroles.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/roles/createUser",
                                "/roles/authenticate",
                                "/roles/forgotPassword",
                                "/roles/resetPassword",
                                "/roles/verifyResetToken",
                                "/roles/2fa/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
