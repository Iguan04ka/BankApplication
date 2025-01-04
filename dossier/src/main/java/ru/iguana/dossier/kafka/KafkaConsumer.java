package ru.iguana.dossier.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    @KafkaListener(topics = "finish-registration", groupId = "dossier")
    public void listen(String message){
        System.out.println(message);
    }
}