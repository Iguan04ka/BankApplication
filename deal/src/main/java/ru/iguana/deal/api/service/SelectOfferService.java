package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.repository.StatementRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class SelectOfferService {
    private final StatementRepository statementRepository;

    public void selectLoanOffer(JsonNode json){
        UUID statementUuid = getStatementIdFromJson(json);

        Optional<Statement> optionalStatement = statementRepository.findById(statementUuid);
        if (optionalStatement.isEmpty()) {
            throw new IllegalArgumentException("No such statement");
        }

        Statement statement = optionalStatement.get();

        statement.getStatusHistory().add(new StatusHistory(
                ApplicationStatus.APPROVED,
                Timestamp.from(Instant.now()),
                ChangeType.AUTOMATIC
        ));

        statement.setStatus(getStatusFromLastStatusHistory(statement));

        statement.setAppliedOffer(json);

        statementRepository.save(statement);
    }

    private UUID getStatementIdFromJson(JsonNode json){
        if (json.has("statementId")) {
            String statementId = json.get("statementId").asText();
            return UUID.fromString(statementId);
        } else {
            throw new IllegalArgumentException("JSON does not contain key 'statementId'");
        }
    }
    private String getStatusFromLastStatusHistory(Statement statement){
        return String.valueOf(statement
                .getStatusHistory()
                .get(statement.getStatusHistory().size() - 1)
                .getStatus().toString());
    }
}
