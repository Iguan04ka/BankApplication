package ru.iguana.deal.api.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.iguana.deal.api.dto.EmailMessageDto;
import ru.iguana.deal.kafka.KafkaProducer;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.EmailTheme;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.util.UUID;

@Service
@AllArgsConstructor
public class DocumentService {

    private final KafkaProducer kafkaProducer;

    private final StatementRepository statementRepository;

    private final ClientRepository clientRepository;

    public void sendDocuments(UUID statementId){
        EmailMessageDto message = new EmailMessageDto()
                .setAddress(getEmailFromClient(statementId))
                .setTheme(EmailTheme.SEND_DOCUMENTS)
                .setStatementId(statementId)
                .setText("Перейти к оформлению документов");

        kafkaProducer.sendMessageToSendDocumentsTopic(message);
    }

    public void signDocuments(UUID statementId){
        EmailMessageDto message = new EmailMessageDto()
                .setAddress(getEmailFromClient(statementId))
                .setTheme(EmailTheme.CREATE_DOCUMENTS)
                .setStatementId(statementId)
                .setText("Документы сформированы");

        kafkaProducer.sendMessageToCreateDocumentsTopic(message);
    }

    public void codeDocuments(UUID statementId){
        EmailMessageDto message = new EmailMessageDto()
                .setAddress(getEmailFromClient(statementId))
                .setTheme(EmailTheme.SEND_SES)
                .setStatementId(statementId)
                .setText("Подпишите документы");

        kafkaProducer.sendMessageToSendSesTopic(message);
    }

    private String getEmailFromClient(UUID statementId){
        Statement statement = statementRepository.findById(statementId).orElseThrow();

        Client client = clientRepository.findById(statement.getClientId()).orElseThrow();

        return client.getEmail();
    }
}
