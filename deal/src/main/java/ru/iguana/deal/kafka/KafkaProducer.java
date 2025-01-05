package ru.iguana.deal.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.iguana.deal.api.dto.EmailMessageDto;

@Service
@AllArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<JsonNode, EmailMessageDto> kafkaTemplate;

    public void sendMessageToFinishRegistrationTopic(EmailMessageDto message){
        kafkaTemplate.send("finish-registration", message);
    }

    public void sendMessageToCreateDocumentsTopic(EmailMessageDto message){
        kafkaTemplate.send("create-documents", message);
    }

    public void sendMessageToSendDocumentsTopic(EmailMessageDto message){
        kafkaTemplate.send("send-documents", message);
    }
    public void sendMessageToSendSesTopic(EmailMessageDto message){
        kafkaTemplate.send("send-ses", message);
    }

    public void sendMessageToCreditIssuedTopic(EmailMessageDto message){
        kafkaTemplate.send("credit-issued", message);
    }

    public void sendMessageToStatementDeniedTopic(EmailMessageDto message){
        kafkaTemplate.send("statement-denied", message);
    }

}
