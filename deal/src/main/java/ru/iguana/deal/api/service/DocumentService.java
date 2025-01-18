package ru.iguana.deal.api.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class DocumentService {

    private final KafkaProducer kafkaProducer;

    private final StatementRepository statementRepository;

    private final ClientRepository clientRepository;

    public void sendDocuments(UUID statementId) {
        log.info("Preparing to send documents for statementId: {}", statementId);
        EmailMessageDto message = new EmailMessageDto()
                .setAddress(getEmailFromClient(statementId))
                .setTheme(EmailTheme.SEND_DOCUMENTS)
                .setStatementId(statementId)
                .setText("Перейти к оформлению документов");

        log.info("Sending message to 'send-documents' topic");
        log.debug("Sending message to 'send-documents' topic: {}", message);
        kafkaProducer.sendMessageToSendDocumentsTopic(message);
    }

    public void signDocuments(UUID statementId) {
        log.info("Preparing to sign documents for statementId: {}", statementId);
        EmailMessageDto message = new EmailMessageDto()
                .setAddress(getEmailFromClient(statementId))
                .setTheme(EmailTheme.SEND_SES)
                .setStatementId(statementId)
                .setText("Документы сформированы");
        log.info("Sending message to 'create-documents' topic");
        log.debug("Sending message to 'create-documents' topic: {}", message);
        kafkaProducer.sendMessageToCreateDocumentsTopic(message);
    }

    public void codeDocuments(UUID statementId) {
        log.info("Preparing to send SES code for statementId: {}", statementId);
        EmailMessageDto message = new EmailMessageDto()
                .setAddress(getEmailFromClient(statementId))
                .setTheme(EmailTheme.CREDIT_ISSUED)
                .setStatementId(statementId)
                .setText("Кредит одобрен");

        log.info("Sending message to 'send-ses' topic");
        log.debug("Sending message to 'send-ses' topic: {}", message);
        kafkaProducer.sendMessageToSendSesTopic(message);
    }

    private String getEmailFromClient(UUID statementId) {
        log.info("Fetching email for statementId: {}", statementId);
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> {
                    log.error("Statement not found for id: {}", statementId);
                    return new RuntimeException("Statement not found");
                });

        Client client = clientRepository.findById(statement.getClientId())
                .orElseThrow(() -> {
                    log.error("Client not found for id: {}", statement.getClientId());
                    return new RuntimeException("Client not found");
                });

        log.debug("Retrieved email: {} for statementId: {}", client.getEmail(), statementId);
        return client.getEmail();
    }
}
