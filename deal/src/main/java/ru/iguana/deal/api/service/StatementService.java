package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class StatementService {
    private final ClientMapper clientMapper;
    private final StatementMapper statementMapper;
    private final ClientRepository clientRepository;
    private final WebClient webClient;
    private final StatementRepository statementRepository;

    @Autowired
    public StatementService(WebClient.Builder webClientBuilder, StatementMapper statementMapper,
                          ClientMapper clientMapper,
                          ClientRepository clientRepository, StatementRepository statementRepository) {

        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
        this.clientMapper = clientMapper;
        this.statementMapper = statementMapper;
        this.clientRepository = clientRepository;
        this.statementRepository = statementRepository;
    }
    public ResponseEntity<List<JsonNode>> getLoanOfferList(JsonNode json) {
        try {
            // Создаем клиента и сохраняем в БД
            ClientDto clientDto = clientMapper.jsonToClientDto(json);
            Client clientEntity = clientMapper.clientDtoToClientEntity(clientDto);
            clientRepository.save(clientEntity);

            // Создаем стейтмент, добавляем id клиента и сохраняем в БД
            StatementDto statementDto = new StatementDto();
            statementDto.setClientId(clientEntity.getClientId());
            statementDto.getStatusHistory()
                    .add(new StatusHistory(
                            ApplicationStatus.PREAPPROVAL,
                            Timestamp.from(Instant.now()),
                            ChangeType.AUTOMATIC
                            ));
            //Устанавливаем статус заявки таким же, как последний статус в списке историй статуса
            statementDto
                    .setStatus
                            (String.valueOf
                                    (statementDto.getStatusHistory()
                                            .get(statementDto.getStatusHistory().size() - 1)
                                                    .getStatus()));
            //Сохраняем стейтмент
            Statement statementEntity = statementMapper.statementDtoToStatementEntity(statementDto);
            statementRepository.save(statementEntity);

            // Получаем лист офферов
            List<JsonNode> loanOffers = fetchLoanOffers(json);

            //Меняем statementId на id statement'а
            loanOffers.forEach(offer -> {
                ObjectNode mutableOffer = (ObjectNode) offer;
                mutableOffer.put("statementId", statementEntity.getStatementId().toString());
            });

            return ResponseEntity.ok(loanOffers);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    private List<JsonNode> fetchLoanOffers(JsonNode loanStatementRequest) {
        JsonNode response = webClient.post()
                .uri("/calculator/offers")
                .bodyValue(loanStatementRequest)
                .retrieve()
                .bodyToMono(JsonNode.class) // Получаем JSON в виде JsonNode
                .block();

        // Преобразуем массив JsonNode в список
        if (response != null && response.isArray()) {
            return StreamSupport.stream(response.spliterator(), false)
                    .collect(Collectors.toList());
        }
        throw new IllegalStateException("Invalid response structure: expected JSON array");
    }
}
