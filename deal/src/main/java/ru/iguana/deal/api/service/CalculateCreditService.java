package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.iguana.deal.api.convertor.ClientConvertor;
import ru.iguana.deal.api.convertor.ScoringDataDtoConvertor;
import ru.iguana.deal.api.dto.CreditDto;
import ru.iguana.deal.api.dto.EmailMessageDto;
import ru.iguana.deal.api.dto.FinishRegistrationRequestDto;
import ru.iguana.deal.api.convertor.CreditConvertor;
import ru.iguana.deal.kafka.KafkaProducer;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Credit;
import ru.iguana.deal.model.entity.Jsonb.Passport;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.entity.enums.CreditStatus;
import ru.iguana.deal.model.entity.enums.EmailTheme;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.CreditRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.security.SecureRandom;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CalculateCreditService {

    public static final int SES_CODE_TTL_MINUTES = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StatementRepository statementRepository;
    private final ClientRepository clientRepository;
    private final CreditRepository creditRepository;
    private final WebClient webClient;
    private final KafkaProducer kafkaProducer;
    private final CreditConvertor creditConvertor;
    private final ClientConvertor clientConvertor;
    private final ScoringDataDtoConvertor scoringDataDtoConvertor;

    public CalculateCreditService(StatementRepository statementRepository,
                                  ClientRepository clientRepository,
                                  CreditConvertor creditConvertor,
                                  CreditRepository creditRepository,
                                  ScoringDataDtoConvertor scoringDataDtoConvertor,
                                  KafkaProducer kafkaProducer,
                                  WebClient webClient,
                                  ClientConvertor clientConvertor) {
        this.statementRepository = statementRepository;
        this.clientRepository = clientRepository;
        this.kafkaProducer = kafkaProducer;
        this.webClient = webClient;
        this.creditConvertor = creditConvertor;
        this.creditRepository = creditRepository;
        this.scoringDataDtoConvertor = scoringDataDtoConvertor;
        this.clientConvertor = clientConvertor;
    }

    @Transactional
    public void calculate(FinishRegistrationRequestDto finishRegistrationRequestDto,
                          String statementId) {
        log.info("Starting credit calculation for statementId: {}", statementId);

        Statement statement = getStatementByStatementId(statementId);
        Client client = getClientByClientIdInStatement(statement);
        log.info("Statement and Client successfully retrieved for statementId: {}", statementId);

        mergeClientFromRegistration(client, finishRegistrationRequestDto);

        JsonNode scoringDataDto = scoringDataDtoConvertor.createScoringDataDto(finishRegistrationRequestDto, client, statement.getStatementId());
        log.debug("ScoringDataDto created: {}", scoringDataDto);

        JsonNode creditJson = getCreditDtoFromCalculator(scoringDataDto);
        log.info("Received response from calculator service: {}", creditJson);

        CreditDto creditDto = creditConvertor.jsonToCreditDto(creditJson);
        creditDto.setCreditStatus(String.valueOf(CreditStatus.CALCULATED));

        Credit creditEntity = creditConvertor.CreditDtoToCreditEntity(creditDto);
        creditRepository.save(creditEntity);
        log.info("Credit entity saved: {}", creditEntity);

        statement.setStatus(String.valueOf(ApplicationStatus.CC_APPROVED));
        statement.getStatusHistory().add(new StatusHistory(ApplicationStatus.CC_APPROVED,
                Timestamp.from(Instant.now()), ChangeType.AUTOMATIC));
        statement.setCredit(creditEntity.getCreditId());

        // Generate one-time SES code with TTL and persist it on the statement
        String code = generateSesCode();
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(SES_CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        statement.setSesCode(code);
        statement.setSesCodeExpiresAt(expiresAt);
        log.info("Generated SES code for statementId: {} (expires at {})", statementId, expiresAt);

        statementRepository.save(statement);
        clientRepository.save(client);

        sendSesCodeEmail(statement, client, creditEntity, code);
        log.info("Statement updated with status: {}", ApplicationStatus.CC_APPROVED);
    }

    private Statement getStatementByStatementId(String statementId) {
        UUID statementUuid = UUID.fromString(statementId);
        Optional<Statement> optionalStatement = statementRepository.findById(statementUuid);
        if (optionalStatement.isEmpty()) {
            log.error("Statement not found for ID: {}", statementId);
            throw new IllegalArgumentException("No such id");
        }
        return optionalStatement.get();
    }

    private Client getClientByClientIdInStatement(Statement statement) {
        Optional<Client> optionalClient = clientRepository.findById(statement.getClientId());
        if (optionalClient.isEmpty()) {
            log.error("Client not found for statementId: {}", statement.getStatementId());
            throw new IllegalArgumentException("No such id");
        }
        return optionalClient.get();
    }

    private JsonNode getCreditDtoFromCalculator(JsonNode scoringDataDto) {
        return webClient.post()
                .uri("/calculator/calc")
                .bodyValue(scoringDataDto)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    private String generateSesCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private void sendSesCodeEmail(Statement statement, Client client, Credit credit, String code) {
        EmailMessageDto message = new EmailMessageDto()
                .setAddress(client.getEmail())
                .setTheme(EmailTheme.SEND_SES)
                .setStatementId(statement.getStatementId())
                .setText("Код подтверждения оформления кредита")
                .setFirstName(client.getFirstName())
                .setMiddleName(client.getMiddleName())
                .setAmount(credit.getAmount())
                .setTerm(credit.getTerm())
                .setMonthlyPayment(credit.getMonthlyPayment())
                .setCode(code)
                .setTtlMinutes(SES_CODE_TTL_MINUTES);

        kafkaProducer.sendMessageToSendSesTopic(message);
    }

    private void mergeClientFromRegistration(Client client, FinishRegistrationRequestDto dto) {
        if (client.getGender() == null)        client.setGender(dto.getGender());
        if (client.getMaritalStatus() == null) client.setMaritalStatus(dto.getMaritalStatus());
        if (client.getDependentAmount() == null) client.setDependentAmount(dto.getDependentAmount());
        if (client.getAccountNumber() == null) client.setAccountNumber(dto.getAccountNumber());
        if (client.getEmployment() == null)
            client.setEmployment(clientConvertor.employmentJsonToDto(dto.getEmployment()));

        Passport existing = client.getPassport();
        Passport merged = new Passport();
        if (existing != null) {
            merged.setSeries(existing.getSeries())
                  .setNumber(existing.getNumber())
                  .setIssueBranch(existing.getIssueBranch())
                  .setIssueDate(existing.getIssueDate());
        }
        if (merged.getIssueDate() == null && dto.getPassportIssueDate() != null) {
            merged.setIssueDate(Date.valueOf(dto.getPassportIssueDate()));
        }
        if (merged.getIssueBranch() == null && dto.getPassportIssueBranch() != null) {
            merged.setIssueBranch(dto.getPassportIssueBranch());
        }
        client.setPassport(merged);
    }
}
