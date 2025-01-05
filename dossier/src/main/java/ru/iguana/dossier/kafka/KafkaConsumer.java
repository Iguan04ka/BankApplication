package ru.iguana.dossier.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.iguana.dossier.service.MailSenderService;

@Service
@AllArgsConstructor
public class KafkaConsumer {
    private final MailSenderService mailSenderService;

    private final MessageConvertor messageConvertor;

    @KafkaListener(topics = "finish-registration", groupId = "dossier")
    public void listenFinishRegistrationTopic(String message){

    }

    @KafkaListener(topics = "send-documents", groupId = "dossier")
    public void listenSendDocumentsTopic(String message){

    }

    @KafkaListener(topics = "create-documents", groupId = "dossier")
    public void listenSignDocumentsTopic(String message){
        
    }

    @KafkaListener(topics = "send-ses", groupId = "dossier")
    public void listenSendSesTopic(String message){
        System.out.println("Received message from Kafka: " + message);
        mailSenderService.sendEmail(
                messageConvertor.getAddress(message),
                messageConvertor.getTheme(message),
                messageConvertor.getText(message));
    }
}
