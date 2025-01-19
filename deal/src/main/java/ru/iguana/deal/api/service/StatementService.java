package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.iguana.deal.api.config.DealProperties;
import ru.iguana.deal.api.convertor.ClientConvertor;
import ru.iguana.deal.api.convertor.StatementConvertor;
import ru.iguana.deal.api.dto.ClientDto;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class StatementService {
    private final ClientConvertor clientConvertor;
    private final StatementConvertor statementConvertor;
    private final ClientRepository clientRepository;
    private final WebClient webClient;
    private final StatementRepository statementRepository;

    private final DealProperties dealProperties;

    public StatementService(WebClient webClient,
                            StatementConvertor statementConvertor,
                            ClientConvertor clientConvertor,
                            ClientRepository clientRepository,
                            StatementRepository statementRepository,
                            DealProperties dealProperties) {
        this.dealProperties = dealProperties;
        this.webClient = webClient;
        this.clientConvertor = clientConvertor;
        this.statementConvertor = statementConvertor;
        this.clientRepository = clientRepository;
        this.statementRepository = statementRepository;
    }

    public ResponseEntity<List<JsonNode>> getLoanOfferList(JsonNode json) {
        log.info("Received request to fetch loan offers with data");
        log.debug("Received request to fetch loan offers with data: {}", json);
        try {
            // Создаем клиента и сохраняем в БД
            ClientDto clientDto = clientConvertor.jsonToClientDto(json);
            Client clientEntity = clientConvertor.clientDtoToClientEntity(clientDto);
            clientRepository.save(clientEntity);
            log.info("Client successfully created and saved with ID: {}", clientEntity.getClientId());

            // Создаем statementDto, добавляем id клиента и статус
            StatementDto statementDto = createStatementDto(clientEntity);

            // Сохраняем стейтмент
            Statement statementEntity = statementConvertor.statementDtoToStatementEntity(statementDto);
            statementRepository.save(statementEntity);
            log.info("Statement successfully created and saved with ID: {}", statementEntity.getStatementId());

            // Получаем лист офферов
            List<JsonNode> loanOffers = fetchLoanOffers(json);

            // Меняем statementId на id statement'а
            changeStatementId(loanOffers, statementEntity);
            log.info("Successfully fetched and updated loan offers for statement ID: {}", statementEntity.getStatementId());

            return ResponseEntity.ok(loanOffers);

        } catch (Exception e) {
            log.error("Error occurred while fetching loan offers: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    private List<JsonNode> fetchLoanOffers(JsonNode loanStatementRequest) {
        log.info("Fetching loan offers with request");
        log.debug("Fetching loan offers with request: {}", loanStatementRequest);
        JsonNode response = webClient.post()
                .uri("/calculator/offers")
                .bodyValue(loanStatementRequest)
                .retrieve()
                .bodyToMono(JsonNode.class) // Получаем JSON в виде JsonNode
                .block();

        if (response != null && response.isArray()) {
            log.info("Loan offers fetched successfully");
            return StreamSupport.stream(response.spliterator(), false)
                    .collect(Collectors.toList());
        } else {
            log.error("Invalid response structure: expected JSON array");
            throw new IllegalStateException("Invalid response structure: expected JSON array");
        }
    }

    private StatementDto createStatementDto(Client clientEntity){
        StatementDto statementDto = new StatementDto();
        statementDto.setClientId(clientEntity.getClientId());
        statementDto.getStatusHistory()
                .add(new StatusHistory(
                        ApplicationStatus.PREAPPROVAL,
                        Timestamp.from(Instant.now()),
                        ChangeType.AUTOMATIC
                ));
        // Устанавливаем статус заявки таким же, как последний статус в списке историй статуса
        String status = String.valueOf(statementDto.getStatusHistory()
                .get(statementDto.getStatusHistory().size() - 1)
                .getStatus());
        statementDto.setStatus(status);
        log.info("Statement status set to: {}", status);

        return statementDto;
    }

    private void changeStatementId(List<JsonNode> loanOffers, Statement statementEntity){
        loanOffers.forEach(offer -> {
            ObjectNode mutableOffer = (ObjectNode) offer;
            mutableOffer.put("statementId", statementEntity.getStatementId().toString());
        });
    }
}
