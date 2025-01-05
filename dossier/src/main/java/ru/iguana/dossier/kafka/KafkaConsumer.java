package ru.iguana.dossier.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    @KafkaListener(topics = "finish-registration", groupId = "dossier")
    public void listenFinishRegistrationTopic(String message){
        System.out.println(message);
    }

    @KafkaListener(topics = "send-documents", groupId = "dossier")
    public void listenSendDocumentsTopic(String message){
        System.out.println(message);
    }

    @KafkaListener(topics = "create-documents", groupId = "dossier")
    public void listenSignDocumentsTopic(String message){
        System.out.println(message);
    }

    @KafkaListener(topics = "send-ses", groupId = "dossier")
    public void listenSendSesTopic(String message){
        System.out.println(message.getClass());
    }
}