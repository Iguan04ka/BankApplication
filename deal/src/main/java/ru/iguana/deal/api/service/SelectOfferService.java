package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.iguana.deal.api.dto.EmailMessageDto;
import ru.iguana.deal.kafka.KafkaProducer;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.entity.enums.EmailTheme;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class SelectOfferService {
    private final StatementRepository statementRepository;

    private final ClientRepository clientRepository;

    private final KafkaProducer kafkaProducer;

    public void selectLoanOffer(JsonNode json) {
        log.info("Received request to select loan offer");
        log.debug("Received request to select loan offer: {}", json);

        try {
            //получаем id стейтмента из json'а
            UUID statementUuid = getStatementIdFromJson(json);
            log.info("Extracted statementId: {}", statementUuid);

            // получаем стейтмент по id и меняем ему статус
            Statement statement = getStatementByStatementId(statementUuid);
            statement.getStatusHistory().add(new StatusHistory(
                    ApplicationStatus.APPROVED,
                    Timestamp.from(Instant.now()),
                    ChangeType.AUTOMATIC
            ));
            // Устанавливаем стейтменту последний статус из истории статусов
            String newStatus = getStatusFromLastStatusHistory(statement);
            statement.setStatus(newStatus);
            log.info("Updated statement status to: {}", newStatus);

            // Устанавливаем стейтменту принятый оффер и сохраняем
            statement.setAppliedOffer(json);
            //отправляем EmailMessage в кафку
            sendEmailMessageDtoToKafka(json);
            log.info("Set applied offer for statementId: {}", statementUuid);
            statementRepository.save(statement);
            log.info("Statement successfully saved for statementId: {}", statementUuid);
        } catch (Exception e) {
            log.error("Error while selecting loan offer: {}", e.getMessage(), e);
            throw e;
        }
    }

    private UUID getStatementIdFromJson(JsonNode json) {
        if (json.has("statementId")) {
            String statementId = json.get("statementId").asText();
            log.info("Extracted statementId from JSON: {}", statementId);
            return UUID.fromString(statementId);
        } else {
            log.error("JSON does not contain key 'statementId'");
            throw new IllegalArgumentException("JSON does not contain key 'statementId'");
        }
    }

    private String getStatusFromLastStatusHistory(Statement statement) {
        String status = String.valueOf(statement
                .getStatusHistory()
                .get(statement.getStatusHistory().size() - 1)
                .getStatus().toString());
        log.info("Extracted status from last status history: {}", status);
        return status;
    }

    private Statement getStatementByStatementId(UUID statementUuid){
        Optional<Statement> optionalStatement = statementRepository.findById(statementUuid);
        if (optionalStatement.isEmpty()) {
            log.error("No statement found for statementId: {}", statementUuid);
            throw new IllegalArgumentException("No such statement");
        }

        return optionalStatement.get();
    }

    private void sendEmailMessageDtoToKafka(JsonNode json){
        Statement statement = statementRepository.findById(getStatementIdFromJson(json)).orElseThrow();

        Client client = clientRepository.findById(statement.getClientId()).orElseThrow();

        EmailMessageDto message = new EmailMessageDto()
                .setAddress(client.getEmail())
                .setTheme(EmailTheme.FINISH_REGISTRATION)
                .setStatementId(statement.getStatementId())
                .setText("Завершите оформление");

        kafkaProducer.sendMessageToFinishRegistrationTopic(message);
    }
}
