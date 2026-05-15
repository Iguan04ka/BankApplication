package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.iguana.deal.api.dto.ClientProfileDto;
import ru.iguana.deal.api.dto.ClientUpdateRequestDto;
import ru.iguana.deal.api.dto.CreditResponseDto;
import ru.iguana.deal.api.dto.StatementDetailDto;
import ru.iguana.deal.api.dto.StatementShortDto;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Credit;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.CreditRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserClientService {

    private final ClientRepository clientRepository;
    private final StatementRepository statementRepository;
    private final CreditRepository creditRepository;

    public ClientProfileDto getProfile(String userSub) {
        log.info("Fetching profile for userSub: {}", userSub);
        Client client = findClientByUserSub(userSub);
        return toClientProfileDto(client);
    }

    public ClientProfileDto updateProfile(String userSub, ClientUpdateRequestDto update) {
        log.info("Updating profile for userSub: {}", userSub);
        Client client = findClientByUserSub(userSub);
        applyProfileUpdates(client, update);
        clientRepository.save(client);
        log.info("Profile updated for userSub: {}", userSub);
        return toClientProfileDto(client);
    }

    public List<StatementShortDto> getStatements(String userSub) {
        log.info("Fetching statements for userSub: {}", userSub);
        Client client = findClientByUserSub(userSub);
        List<Statement> statements = statementRepository.findAllByClientId(client.getClientId());
        log.info("Found {} statements for userSub: {}", statements.size(), userSub);
        return statements.stream()
                .map(this::toStatementShortDto)
                .collect(Collectors.toList());
    }

    public StatementDetailDto getStatement(String userSub, UUID statementId) {
        log.info("Fetching statement {} for userSub: {}", statementId, userSub);
        Client client = findClientByUserSub(userSub);
        Statement statement = findStatementWithOwnershipCheck(statementId, client.getClientId());
        return toStatementDetailDto(statement);
    }

    public void denyStatement(String userSub, UUID statementId) {
        log.info("Denying statement {} for userSub: {}", statementId, userSub);
        Client client = findClientByUserSub(userSub);
        Statement statement = findStatementWithOwnershipCheck(statementId, client.getClientId());

        Set<String> deniableStatuses = Set.of(
                ApplicationStatus.PREAPPROVAL.name(),
                ApplicationStatus.APPROVED.name(),
                ApplicationStatus.CC_APPROVED.name()
        );
        if (!deniableStatuses.contains(statement.getStatus())) {
            log.warn("Cannot deny statement {} with status: {}", statementId, statement.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot deny statement in status: " + statement.getStatus()
            );
        }

        statement.setStatus(ApplicationStatus.CLIENT_DENIED.name());
        statement.getStatusHistory().add(new StatusHistory(
                ApplicationStatus.CLIENT_DENIED,
                Timestamp.from(Instant.now()),
                ChangeType.MANUAL
        ));
        statementRepository.save(statement);
        log.info("Statement {} denied by client", statementId);
    }

    public List<CreditResponseDto> getCredits(String userSub) {
        log.info("Fetching credits for userSub: {}", userSub);
        Client client = findClientByUserSub(userSub);
        List<Statement> statements = statementRepository.findAllByClientId(client.getClientId());
        List<CreditResponseDto> credits = statements.stream()
                .map(Statement::getCredit)
                .filter(Objects::nonNull)
                .distinct()
                .map(creditRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toCreditResponseDto)
                .collect(Collectors.toList());
        log.info("Found {} credits for userSub: {}", credits.size(), userSub);
        return credits;
    }

    public CreditResponseDto getCredit(String userSub, UUID creditId) {
        log.info("Fetching credit {} for userSub: {}", creditId, userSub);
        Client client = findClientByUserSub(userSub);
        List<Statement> statements = statementRepository.findAllByClientId(client.getClientId());
        boolean isOwner = statements.stream()
                .anyMatch(s -> creditId.equals(s.getCredit()));
        if (!isOwner) {
            log.warn("Access denied: credit {} does not belong to userSub: {}", creditId, userSub);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit not found"));
        return toCreditResponseDto(credit);
    }

    private Client findClientByUserSub(String userSub) {
        return clientRepository.findByUserSub(userSub)
                .orElseThrow(() -> {
                    log.warn("Client not found for userSub: {}", userSub);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Client profile not found");
                });
    }

    private Statement findStatementWithOwnershipCheck(UUID statementId, UUID clientId) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));
        if (!statement.getClientId().equals(clientId)) {
            log.warn("Access denied: statement {} does not belong to clientId: {}", statementId, clientId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return statement;
    }

    private void applyProfileUpdates(Client client, ClientUpdateRequestDto update) {
        if (update.getLastName() != null) client.setLastName(update.getLastName());
        if (update.getFirstName() != null) client.setFirstName(update.getFirstName());
        if (update.getMiddleName() != null) client.setMiddleName(update.getMiddleName());
        if (update.getBirthDate() != null) client.setBirthDate(update.getBirthDate());
        if (update.getEmail() != null) client.setEmail(update.getEmail());
        if (update.getGender() != null) client.setGender(update.getGender());
        if (update.getMaritalStatus() != null) client.setMaritalStatus(update.getMaritalStatus());
        if (update.getDependentAmount() != null) client.setDependentAmount(update.getDependentAmount());
        if (update.getPassport() != null) client.setPassport(update.getPassport());
        if (update.getEmployment() != null) client.setEmployment(update.getEmployment());
        if (update.getAccountNumber() != null) client.setAccountNumber(update.getAccountNumber());
    }

    private ClientProfileDto toClientProfileDto(Client client) {
        return new ClientProfileDto()
                .setLastName(client.getLastName())
                .setFirstName(client.getFirstName())
                .setMiddleName(client.getMiddleName())
                .setBirthDate(client.getBirthDate())
                .setEmail(client.getEmail())
                .setGender(client.getGender())
                .setMaritalStatus(client.getMaritalStatus())
                .setDependentAmount(client.getDependentAmount())
                .setPassport(client.getPassport())
                .setEmployment(client.getEmployment())
                .setAccountNumber(client.getAccountNumber());
    }

    private StatementShortDto toStatementShortDto(Statement statement) {
        StatementShortDto dto = new StatementShortDto()
                .setStatementId(statement.getStatementId())
                .setStatus(statement.getStatus())
                .setCreationDate(statement.getCreationDate());

        JsonNode offer = statement.getAppliedOffer();
        if (offer != null && !offer.isNull()) {
            JsonNode amountNode = offer.path("requestedAmount");
            if (!amountNode.isMissingNode() && !amountNode.isNull()) {
                dto.setRequestedAmount(new BigDecimal(amountNode.asText()));
            }
            JsonNode termNode = offer.path("term");
            if (!termNode.isMissingNode() && !termNode.isNull()) {
                dto.setTerm(termNode.intValue());
            }
        } else {
            // PREAPPROVAL: offer not yet selected — use the values stored at creation time
            dto.setRequestedAmount(statement.getRequestedAmount());
            dto.setTerm(statement.getRequestedTerm());
        }
        return dto;
    }

    private StatementDetailDto toStatementDetailDto(Statement statement) {
        StatementDetailDto dto = new StatementDetailDto()
                .setStatementId(statement.getStatementId())
                .setStatus(statement.getStatus())
                .setCreationDate(statement.getCreationDate())
                .setSignDate(statement.getSignDate())
                .setAppliedOffer(statement.getAppliedOffer())
                .setStatusHistory(statement.getStatusHistory());

        if (statement.getCredit() != null) {
            creditRepository.findById(statement.getCredit())
                    .ifPresent(credit -> dto.setCredit(toCreditResponseDto(credit)));
        }
        return dto;
    }

    private CreditResponseDto toCreditResponseDto(Credit credit) {
        return new CreditResponseDto()
                .setCreditId(credit.getCreditId())
                .setAmount(credit.getAmount())
                .setTerm(credit.getTerm())
                .setMonthlyPayment(credit.getMonthlyPayment())
                .setRate(credit.getRate())
                .setPsk(credit.getPsk())
                .setPaymentSchedule(credit.getPaymentSchedule())
                .setIsInsuranceEnabled(credit.getIsInsuranceEnabled())
                .setIsSalaryClient(credit.getIsSalaryClient())
                .setCreditStatus(credit.getCreditStatus());
    }
}
