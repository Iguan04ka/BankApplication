package ru.iguana.deal.kafka;

import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.iguana.deal.api.dto.EmailMessageDto;

@Service
@AllArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, EmailMessageDto> kafkaTemplate;

    public void sendMessageToSendSesTopic(EmailMessageDto message){
        kafkaTemplate.send("send-ses", message);
    }

    public void sendMessageToFinishRegistrationTopic(EmailMessageDto message){
        kafkaTemplate.send("finish-registration", message);
    }
}
