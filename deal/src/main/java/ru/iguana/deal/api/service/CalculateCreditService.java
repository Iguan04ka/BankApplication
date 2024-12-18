package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.iguana.deal.api.config.DealProperties;
import ru.iguana.deal.api.convertor.ScoringDataDtoConvertor;
import ru.iguana.deal.api.dto.CreditDto;
import ru.iguana.deal.api.dto.FinishRegistrationRequestDto;
import ru.iguana.deal.api.convertor.CreditConvertor;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Credit;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.entity.enums.CreditStatus;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.CreditRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CalculateCreditService {

    private final StatementRepository statementRepository;
    private final ClientRepository clientRepository;
    private final CreditRepository creditRepository;
    private final WebClient webClient;

    private final DealProperties dealProperties;
    private final CreditConvertor creditConvertor;

    private final ScoringDataDtoConvertor scoringDataDtoConvertor;

    public CalculateCreditService(StatementRepository statementRepository,
                                  ClientRepository clientRepository,
                                  CreditConvertor creditConvertor,
                                  CreditRepository creditRepository,
                                  ScoringDataDtoConvertor scoringDataDtoConvertor,
                                  DealProperties dealProperties,
                                  WebClient webClient) {
        this.statementRepository = statementRepository;
        this.clientRepository = clientRepository;
        this.dealProperties = dealProperties;
        this.webClient = webClient;
        this.creditConvertor = creditConvertor;
        this.creditRepository = creditRepository;
        this.scoringDataDtoConvertor = scoringDataDtoConvertor;
    }

    public void calculate(FinishRegistrationRequestDto finishRegistrationRequestDto,
                          String statementId) {
        log.info("Starting credit calculation for statementId: {}", statementId);

        UUID statementUuid = UUID.fromString(statementId);

        Optional<Statement> optionalStatement = statementRepository.findById(statementUuid);
        if (optionalStatement.isEmpty()) {
            log.error("Statement not found for ID: {}", statementId);
            throw new IllegalArgumentException("No such id");
        }

        Statement statement = optionalStatement.get();
        Optional<Client> optionalClient = clientRepository.findById(statement.getClientId());
        if (optionalClient.isEmpty()) {
            log.error("Client not found for statementId: {}", statementId);
            throw new IllegalArgumentException("No such id");
        }

        Client client = optionalClient.get();
        log.info("Statement and Client successfully retrieved for statementId: {}", statementId);

        // Create ScoringDataDto
        JsonNode scoringDataDto = scoringDataDtoConvertor.createScoringDataDto(finishRegistrationRequestDto, client);
        log.debug("ScoringDataDto created: {}", scoringDataDto);

        // Send scoring data to calculator service
        JsonNode creditJson = webClient.post()
                .uri("/calculator/calc")
                .bodyValue(scoringDataDto)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        log.info("Received response from calculator service: {}", creditJson);

        CreditDto creditDto = creditConvertor.jsonToCreditDto(creditJson);
        creditDto.setCreditStatus(String.valueOf(CreditStatus.CALCULATED));

        Credit creditEntity = creditConvertor.CreditDtoToCreditEntity(creditDto);
        creditRepository.save(creditEntity);
        log.info("Credit entity saved: {}", creditEntity);

        statement.setStatus(String.valueOf(ApplicationStatus.CC_APPROVED));
        statement.getStatusHistory().add(new StatusHistory(ApplicationStatus.CC_APPROVED,
                Timestamp.from(Instant.now()), ChangeType.AUTOMATIC));

        statementRepository.save(statement);
        log.info("Statement updated with status: {}", ApplicationStatus.CC_APPROVED);
    }

}
