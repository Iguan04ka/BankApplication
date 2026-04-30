package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    private final ObjectMapper objectMapper;

    public StatementService(WebClient webClient,
                            StatementConvertor statementConvertor,
                            ClientConvertor clientConvertor,
                            ClientRepository clientRepository,
                            StatementRepository statementRepository,
                            DealProperties dealProperties,
                            ObjectMapper objectMapper) {
        this.dealProperties = dealProperties;
        this.webClient = webClient;
        this.clientConvertor = clientConvertor;
        this.statementConvertor = statementConvertor;
        this.clientRepository = clientRepository;
        this.statementRepository = statementRepository;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<List<JsonNode>> getLoanOfferList(JsonNode json) {
        log.info("Received request to fetch loan offers with data");
        log.debug("Received request to fetch loan offers with data: {}", json);
        try {
            // Создаем DTO
            ClientDto clientDto = clientConvertor.jsonToClientDto(json);

            // Получаем клиента по sub
            Client clientEntity = clientRepository.findByUserSub(clientDto.getUserSub())
                    .orElseThrow(() -> new IllegalArgumentException("Client not found"));

            log.info("Client found with ID: {}", clientEntity.getClientId());

            mergeClientFromDto(clientEntity, clientDto);

            clientRepository.save(clientEntity);

            // Создаем statementDto
            StatementDto statementDto = createStatementDto(clientEntity);

            // Сохраняем сумму и срок запроса для возможности повторной генерации предложений
            if (json.has("amount") && !json.get("amount").isNull()) {
                statementDto.setRequestedAmount(new BigDecimal(json.get("amount").asText()));
            }
            if (json.has("term") && !json.get("term").isNull()) {
                statementDto.setRequestedTerm(json.get("term").intValue());
            }

            // Сохраняем стейтмент
            Statement statementEntity = statementConvertor.statementDtoToStatementEntity(statementDto);
            statementRepository.save(statementEntity);
            log.info("Statement successfully created and saved with ID: {}", statementEntity.getStatementId());

            // Получаем лист офферов
            List<JsonNode> loanOffers = fetchLoanOffers(json);

            // Меняем statementId
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

    // Fills in only the fields that are currently null in the client entity.
    // Existing (non-null) profile data is never overwritten by incoming request data.
    private void mergeClientFromDto(Client clientEntity, ClientDto clientDto) {
        if (clientEntity.getLastName() == null)      clientEntity.setLastName(clientDto.getLastName());
        if (clientEntity.getFirstName() == null)     clientEntity.setFirstName(clientDto.getFirstName());
        if (clientEntity.getMiddleName() == null)    clientEntity.setMiddleName(clientDto.getMiddleName());
        if (clientEntity.getBirthDate() == null)     clientEntity.setBirthDate(clientDto.getBirthDate());
        if (clientEntity.getEmail() == null)         clientEntity.setEmail(clientDto.getEmail());
        if (clientEntity.getGender() == null)        clientEntity.setGender(clientDto.getGender());
        if (clientEntity.getMaritalStatus() == null) clientEntity.setMaritalStatus(clientDto.getMaritalStatus());
        if (clientEntity.getDependentAmount() == null) clientEntity.setDependentAmount(clientDto.getDependentAmount());
        if (clientEntity.getPassport() == null)      clientEntity.setPassport(clientDto.getPassport());
        if (clientEntity.getEmployment() == null)    clientEntity.setEmployment(clientDto.getEmployment());
        if (clientEntity.getAccountNumber() == null) clientEntity.setAccountNumber(clientDto.getAccountNumber());
    }

    public ResponseEntity<List<JsonNode>> getOffersForPreapprovalStatement(
            UUID statementId, String userSub, BigDecimal fallbackAmount, Integer fallbackTerm) {
        log.info("Re-generating offers for PREAPPROVAL statementId: {}", statementId);

        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));

        if (!ApplicationStatus.PREAPPROVAL.name().equals(statement.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Statement is not in PREAPPROVAL status: " + statement.getStatus());
        }

        Client client = clientRepository.findByUserSub(userSub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        if (!statement.getClientId().equals(client.getClientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Use stored values if available, otherwise fall back to the values provided by the caller
        BigDecimal amount = statement.getRequestedAmount() != null ? statement.getRequestedAmount() : fallbackAmount;
        Integer term    = statement.getRequestedTerm()    != null ? statement.getRequestedTerm()    : fallbackTerm;

        if (amount == null || term == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot determine loan parameters: provide amount and term");
        }

        ObjectNode loanRequest = objectMapper.createObjectNode();
        loanRequest.put("amount", amount);
        loanRequest.put("term", term);
        loanRequest.put("firstName", client.getFirstName());
        loanRequest.put("lastName", client.getLastName());
        loanRequest.put("middleName", client.getMiddleName());
        loanRequest.put("email", client.getEmail());
        if (client.getBirthDate() != null) {
            loanRequest.put("birthdate", client.getBirthDate().toString());
        }
        if (client.getPassport() != null) {
            loanRequest.put("passportSeries", client.getPassport().getSeries());
            loanRequest.put("passportNumber", client.getPassport().getNumber());
        }

        List<JsonNode> loanOffers = fetchLoanOffers(loanRequest);
        changeStatementId(loanOffers, statement);
        log.info("Successfully re-generated {} offers for statementId: {}", loanOffers.size(), statementId);
        return ResponseEntity.ok(loanOffers);
    }
}
