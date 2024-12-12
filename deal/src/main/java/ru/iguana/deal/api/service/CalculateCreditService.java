package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.iguana.deal.api.dto.CreditDto;
import ru.iguana.deal.api.dto.FinishRegistrationRequestDto;
import ru.iguana.deal.api.mapper.CreditMapper;
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
public class CalculateCreditService {

    private final StatementRepository statementRepository;

    private final ClientRepository clientRepository;

    private final CreditRepository creditRepository;

    private final ObjectMapper objectMapper;

    private final WebClient webClient;
    private final CreditMapper creditMapper;


    public CalculateCreditService(StatementRepository statementRepository,
                                  ClientRepository clientRepository,
                                  ObjectMapper objectMapper,
                                  WebClient.Builder webClientBuilder,
                                  CreditMapper creditMapper,
                                  CreditRepository creditRepository) {
        this.statementRepository = statementRepository;
        this.clientRepository = clientRepository;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
        this.creditMapper = creditMapper;
        this.creditRepository = creditRepository;
    }

    public void calculate(FinishRegistrationRequestDto finishRegistrationRequestDto,
                                              String statementId){
        //Достаём заявку и клиента из БД по statementId
        UUID statementUuid = UUID.fromString(statementId);
        Optional<Statement> optionalStatement = statementRepository.findById(statementUuid);
        Optional<Client> optionalClient = clientRepository.findById(
                optionalStatement.get().getClientId()
        );

        if (optionalStatement.isEmpty() || optionalClient.isEmpty()) {
            throw new IllegalArgumentException("No such id");
        }

        Statement statement = optionalStatement.get();
        Client client = optionalClient.get();

        JsonNode scoringDataDto = createScoringDataDto(finishRegistrationRequestDto,
                client);

        JsonNode creditJson = webClient.post()
                .uri("/calculator/calc")
                .bodyValue(scoringDataDto)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        CreditDto creditDto = creditMapper.jsonToCreditDto(creditJson);
        creditDto.setCreditStatus(String.valueOf(CreditStatus.CALCULATED));

        Credit creditEntity = creditMapper.CreditDtoToCreditEntity(creditDto);
        creditRepository.save(creditEntity);

        statement.setStatus(String.valueOf(ApplicationStatus.CC_APPROVED));

        statement.getStatusHistory().add(new StatusHistory(ApplicationStatus.CC_APPROVED,
                                            Timestamp.from(Instant.now()),
                                            ChangeType.AUTOMATIC));

        statementRepository.save(statement);

    }

    private JsonNode createScoringDataDto(FinishRegistrationRequestDto finishRegistrationRequestDto,
                                          Client client){

        ObjectNode scoringDataDto = objectMapper.createObjectNode();

        scoringDataDto.put("amount", statementRepository.findAmountByClientId(client.getClientId()));

        scoringDataDto.put("term", statementRepository.findTermByClientId(client.getClientId()));

        scoringDataDto.put("firstName", client.getFirstName());

        scoringDataDto.put("lastName", client.getLastName());

        scoringDataDto.put("middleName", client.getMiddleName());

        scoringDataDto.put("gender", finishRegistrationRequestDto.getGender());

        scoringDataDto.put("birthdate", String.valueOf(client.getBirthDate()));

        scoringDataDto.put("passportSeries", client.getPassport().getSeries());

        scoringDataDto.put("passportNumber", client.getPassport().getNumber());

        scoringDataDto.put("passportIssueDate", String.valueOf(finishRegistrationRequestDto.getPassportIssueDate()));

        scoringDataDto.put("passportIssueBranch", finishRegistrationRequestDto.getPassportIssueBranch());

        scoringDataDto.put("maritalStatus", finishRegistrationRequestDto.getMaritalStatus());

        scoringDataDto.put("dependentAmount", finishRegistrationRequestDto.getDependentAmount());

        scoringDataDto.set("employment", objectMapper.valueToTree(finishRegistrationRequestDto.getEmployment()));

        scoringDataDto.put("accountNumber", finishRegistrationRequestDto.getAccountNumber());

        scoringDataDto.put("isInsuranceEnabled", statementRepository.findIsInsuranceEnabledByClientId(client.getClientId()));

        scoringDataDto.put("isSalaryClient", statementRepository.findIsSalaryClientByClientId(client.getClientId()));

        return scoringDataDto;
    }
}
