package ru.iguana.integrationroles.api.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.iguana.integrationroles.api.dto.PasswordResetEmailDto;
import ru.iguana.integrationroles.api.dto.TwoFactorEmailDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPasswordResetEmail(PasswordResetEmailDto message) {
        log.info("Sending message to 'password-reset' topic for {}", message.getAddress());
        kafkaTemplate.send("password-reset", message);
    }

    public void sendTwoFactorCode(TwoFactorEmailDto message) {
        log.info("Sending message to 'two-factor-code' topic for {} (purpose={})",
                message.getAddress(), message.getPurpose());
        kafkaTemplate.send("two-factor-code", message);
    }
}
