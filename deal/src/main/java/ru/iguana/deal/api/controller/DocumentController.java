package ru.iguana.deal.api.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.iguana.deal.api.dto.EmailMessageDto;
import ru.iguana.deal.kafka.KafkaProducer;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.EmailTheme;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.util.UUID;

@RestController
@RequestMapping("/deal/document/{statementId}")
@AllArgsConstructor
public class DocumentController {

    private final KafkaProducer kafkaProducer;

    private  final StatementRepository statementRepository;

    private final ClientRepository clientRepository;


    //TODO service

    @PostMapping("/send")
    public void send(@PathVariable UUID statementId){
        Statement statement = statementRepository.findById(statementId).orElseThrow();

        Client client = clientRepository.findById(statement.getClientId()).orElseThrow();

        String email = client.getEmail();

        EmailMessageDto message = new EmailMessageDto()
                .setAddress(email)
                .setStatementId(statement.getStatementId())
                .setTheme(EmailTheme.SEND_DOCUMENTS)
                .setText("Send documents");
        kafkaProducer.sendMessageToSendSesTopic(message);
    }
}
