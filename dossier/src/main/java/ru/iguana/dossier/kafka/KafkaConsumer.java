package ru.iguana.dossier.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.iguana.dossier.service.MailSenderService;

@Service
@AllArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final MailSenderService mailSenderService;

    private final MessageConvertor messageConvertor;

    @KafkaListener(topics = "finish-registration", groupId = "dossier")
    public void listenFinishRegistrationTopic(String message){
        log.info("Received message from topic 'finish-registration'");
        log.debug("Received message from topic 'finish-registration': {}", message);
        send(message);
    }

    @KafkaListener(topics = "send-documents", groupId = "dossier")
    public void listenSendDocumentsTopic(String message){
        log.info("Received message from topic 'send-documents'");
        log.debug("Received message from topic 'send-documents': {}", message);
        send(message);
    }

    @KafkaListener(topics = "create-documents", groupId = "dossier")
    public void listenSignDocumentsTopic(String message){
        log.info("Received message from topic 'create-documents'");
        log.debug("Received message from topic 'create-documents': {}", message);
        send(message);
    }

    @KafkaListener(topics = "send-ses", groupId = "dossier")
    public void listenSendSesTopic(String message){
        log.info("Received message from topic 'send-ses'");
        log.debug("Received message from topic 'send-ses': {}", message);
        send(message);
    }

    private void send(String message){
        try {
            String address = messageConvertor.getAddress(message);
            String theme = messageConvertor.getTheme(message);
            String text = messageConvertor.getText(message);

            log.debug("Extracted email details");
            log.debug("Extracted email details - Address: {}, Theme: {}, Text: {}", address, theme, text);
            mailSenderService.sendEmail(address, theme, text);
        } catch (Exception e) {
            log.error("Failed to process message: {}. Error: {}", message, e.getMessage(), e);
        }
    }
}
