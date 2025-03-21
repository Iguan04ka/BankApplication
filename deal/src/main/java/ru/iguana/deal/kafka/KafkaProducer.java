package ru.iguana.deal.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.iguana.deal.api.dto.EmailMessageDto;

@Service
@AllArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, EmailMessageDto> kafkaTemplate;

    public void sendMessageToFinishRegistrationTopic(EmailMessageDto message) {
        log.info("Sending message to topic 'finish-registration'");
        log.debug("Sending message to topic 'finish-registration': {}", message);
        kafkaTemplate.send("finish-registration", message);
    }

    public void sendMessageToCreateDocumentsTopic(EmailMessageDto message) {
        log.info("Sending message to topic 'create-documents'");
        log.debug("Sending message to topic 'create-documents': {}", message);
        kafkaTemplate.send("create-documents", message);
    }

    public void sendMessageToSendDocumentsTopic(EmailMessageDto message) {
        log.info("Sending message to topic 'send-documents'");
        log.debug("Sending message to topic 'send-documents': {}", message);
        kafkaTemplate.send("send-documents", message);
    }

    public void sendMessageToSendSesTopic(EmailMessageDto message) {
        log.info("Sending message to topic 'send-ses'");
        log.debug("Sending message to topic 'send-ses': {}", message);
        kafkaTemplate.send("send-ses", message);
    }

    public void sendMessageToCreditIssuedTopic(EmailMessageDto message) {
        log.info("Sending message to topic 'credit-issued'");
        log.debug("Sending message to topic 'credit-issued': {}", message);
        kafkaTemplate.send("credit-issued", message);
    }

    public void sendMessageToStatementDeniedTopic(EmailMessageDto message) {
        log.info("Sending message to topic 'statement-denied'");
        log.debug("Sending message to topic 'statement-denied': {}", message);
        kafkaTemplate.send("statement-denied", message);
    }

}
