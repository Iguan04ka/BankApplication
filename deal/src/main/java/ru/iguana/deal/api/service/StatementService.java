package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.iguana.deal.api.dto.ClientDto;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.api.mapper.ClientMapper;
import ru.iguana.deal.api.mapper.StatementMapper;
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
    private final ClientMapper clientMapper;
    private final StatementMapper statementMapper;
    private final ClientRepository clientRepository;
    private final WebClient webClientBuilder;
    private final StatementRepository statementRepository;

    public StatementService(WebClient.Builder webClientBuilder,
                            StatementMapper statementMapper,
                            ClientMapper clientMapper,
                            ClientRepository clientRepository,
                            StatementRepository statementRepository) {

        this.webClientBuilder = webClientBuilder.baseUrl("http://localhost:8081").build();
        this.clientMapper = clientMapper;
        this.statementMapper = statementMapper;
        this.clientRepository = clientRepository;
        this.statementRepository = statementRepository;
    }



    public ResponseEntity<List<JsonNode>> getLoanOfferList(JsonNode json) {
        log.info("Received request to fetch loan offers with data");
        log.debug("Received request to fetch loan offers with data: {}", json);
        try {
            // Создаем клиента и сохраняем в БД
            ClientDto clientDto = clientMapper.jsonToClientDto(json);
            Client clientEntity = clientMapper.clientDtoToClientEntity(clientDto);
            clientRepository.save(clientEntity);
            log.info("Client successfully created and saved with ID: {}", clientEntity.getClientId());

            // Создаем стейтмент, добавляем id клиента и сохраняем в БД
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

            // Сохраняем стейтмент
            Statement statementEntity = statementMapper.statementDtoToStatementEntity(statementDto);
            statementRepository.save(statementEntity);
            log.info("Statement successfully created and saved with ID: {}", statementEntity.getStatementId());

            // Получаем лист офферов
            List<JsonNode> loanOffers = fetchLoanOffers(json);

            // Меняем statementId на id statement'а
            loanOffers.forEach(offer -> {
                ObjectNode mutableOffer = (ObjectNode) offer;
                mutableOffer.put("statementId", statementEntity.getStatementId().toString());
            });
            log.info("Successfully fetched and updated loan offers for statement ID: {}", statementEntity.getStatementId());

            return ResponseEntity.ok(loanOffers);

        } catch (Exception e) {
            log.error("Error occurred while fetching loan offers: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    private List<JsonNode> fetchLoanOffers(JsonNode loanStatementRequest) {
        log.info("Fetching loan offers with request: {}");
        log.debug("Fetching loan offers with request: {}", loanStatementRequest);
        JsonNode response = webClientBuilder.post()
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
}
