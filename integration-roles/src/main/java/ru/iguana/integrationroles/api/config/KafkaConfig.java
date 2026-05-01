package ru.iguana.integrationroles.api.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic rolesTopic() {
        return new NewTopic("roles", 1, (short) 1);
    }

    @Bean
    public NewTopic passwordResetTopic() {
        return new NewTopic("password-reset", 1, (short) 1);
    }
}