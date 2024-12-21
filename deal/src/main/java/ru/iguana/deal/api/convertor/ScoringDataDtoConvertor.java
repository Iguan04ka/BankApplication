package ru.iguana.deal.api.convertor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.iguana.deal.api.dto.FinishRegistrationRequestDto;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.repository.StatementRepository;

@Component
@Slf4j
@AllArgsConstructor
public class ScoringDataDtoConvertor {
    private final ObjectMapper objectMapper;

    private final StatementRepository statementRepository;

    public JsonNode createScoringDataDto(FinishRegistrationRequestDto finishRegistrationRequestDto,
                                          Client client) {
        log.info("Creating ScoringDataDto for clientId: {}", client.getClientId());

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

        log.debug("ScoringDataDto created: {}", scoringDataDto);
        return scoringDataDto;
    }
}
