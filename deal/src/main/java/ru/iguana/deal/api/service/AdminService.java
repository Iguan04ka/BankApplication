package ru.iguana.deal.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.iguana.deal.api.convertor.StatementConvertor;
import ru.iguana.deal.api.dto.AdminDashboardStatsDto;
import ru.iguana.deal.api.dto.AdminStatementDetailDto;
import ru.iguana.deal.api.dto.AdminStatementSummaryDto;
import ru.iguana.deal.api.dto.AdminUpdateStatusRequestDto;
import ru.iguana.deal.api.dto.ClientDto;
import ru.iguana.deal.api.dto.CreditResponseDto;
import ru.iguana.deal.api.dto.StatementDto;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final StatementRepository statementRepository;
    private final CreditRepository creditRepository;
    private final ClientRepository clientRepository;
    private final StatementConvertor statementConvertor;

    // ───── statements ────────────────────────────────────────────────────

    public StatementDto getStatement(String statementId) {
        log.info("Fetching statement with ID: {}", statementId);
        Statement statement = statementRepository.findById(UUID.fromString(statementId))
                .orElseThrow(() -> new RuntimeException("Statement not found"));
        return statementConvertor.statementEntityToStatementDto(statement);
    }

    public List<AdminStatementSummaryDto> listStatements(String status,
                                                         Timestamp from,
                                                         Timestamp to) {
        log.info("Admin listStatements status={} from={} to={}", status, from, to);
        List<Statement> statements = statementRepository.findFiltered(status, from, to);
        List<AdminStatementSummaryDto> result = new ArrayList<>(statements.size());
        for (Statement s : statements) {
            result.add(buildSummary(s));
        }
        return result;
    }

    public AdminStatementDetailDto getStatementByCreditId(String creditId) {
        Statement s = statementRepository.findByCredit(UUID.fromString(creditId))
                .orElseThrow(() -> new RuntimeException("Statement not found for credit: " + creditId));
        return getStatementDetail(s.getStatementId().toString());
    }

    public AdminStatementDetailDto getStatementDetail(String statementId) {
        Statement s = statementRepository.findById(UUID.fromString(statementId))
                .orElseThrow(() -> new RuntimeException("Statement not found"));

        AdminStatementDetailDto dto = new AdminStatementDetailDto()
                .setStatementId(s.getStatementId())
                .setStatus(s.getStatus())
                .setCreationDate(s.getCreationDate())
                .setSignDate(s.getSignDate())
                .setRequestedAmount(s.getRequestedAmount())
                .setRequestedTerm(s.getRequestedTerm())
                .setAppliedOffer(s.getAppliedOffer())
                .setStatusHistory(s.getStatusHistory())
                .setClientId(s.getClientId());

        if (s.getClientId() != null) {
            clientRepository.findById(s.getClientId())
                    .ifPresent(c -> {
                        dto.setClient(toClientDto(c));
                        dto.setClientUserSub(c.getUserSub());
                    });
        }

        if (s.getCredit() != null) {
            creditRepository.findById(s.getCredit())
                    .ifPresent(c -> dto.setCredit(toCreditDto(c)));
        }
        return dto;
    }

    public AdminStatementDetailDto updateStatementStatus(String statementId,
                                                         AdminUpdateStatusRequestDto request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        ApplicationStatus newStatus;
        try {
            newStatus = ApplicationStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown status: " + request.getStatus());
        }

        Statement s = statementRepository.findById(UUID.fromString(statementId))
                .orElseThrow(() -> new RuntimeException("Statement not found"));

        s.setStatus(newStatus.name());
        if (s.getStatusHistory() == null) {
            s.setStatusHistory(new ArrayList<>());
        }
        s.getStatusHistory().add(new StatusHistory(
                newStatus,
                Timestamp.from(Instant.now()),
                ChangeType.MANUAL
        ));
        statementRepository.save(s);
        return getStatementDetail(statementId);
    }

    // ───── credits ───────────────────────────────────────────────────────

    public List<CreditResponseDto> listCredits() {
        List<CreditResponseDto> result = new ArrayList<>();
        creditRepository.findAll().forEach(c -> result.add(toCreditDto(c)));
        return result;
    }

    public CreditResponseDto getCredit(String creditId) {
        Credit c = creditRepository.findById(UUID.fromString(creditId))
                .orElseThrow(() -> new RuntimeException("Credit not found"));
        return toCreditDto(c);
    }

    // ───── dashboard ─────────────────────────────────────────────────────

    public AdminDashboardStatsDto dashboardStats() {
        long totalStatements = 0;
        long totalCredits = 0;
        for (Statement ignored : statementRepository.findAll()) totalStatements++;
        for (Credit ignored : creditRepository.findAll()) totalCredits++;

        Map<String, Long> byStatus = new HashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] row : statementRepository.countByStatus()) {
            String key = row[0] == null ? "UNKNOWN" : String.valueOf(row[0]);
            Long count = ((Number) row[1]).longValue();
            byStatus.put(key, count);
        }

        BigDecimal issuedAmount = BigDecimal.ZERO;
        for (Credit c : creditRepository.findAll()) {
            if ("ISSUED".equalsIgnoreCase(c.getCreditStatus()) && c.getAmount() != null) {
                issuedAmount = issuedAmount.add(c.getAmount());
            }
        }

        return new AdminDashboardStatsDto()
                .setTotalStatements(totalStatements)
                .setTotalCredits(totalCredits)
                .setTotalIssuedAmount(issuedAmount)
                .setStatementsByStatus(byStatus);
    }

    // ───── helpers ───────────────────────────────────────────────────────

    private AdminStatementSummaryDto buildSummary(Statement s) {
        AdminStatementSummaryDto dto = new AdminStatementSummaryDto()
                .setStatementId(s.getStatementId())
                .setClientId(s.getClientId())
                .setCreditId(s.getCredit())
                .setStatus(s.getStatus())
                .setCreationDate(s.getCreationDate())
                .setRequestedAmount(s.getRequestedAmount())
                .setRequestedTerm(s.getRequestedTerm());

        if (s.getClientId() != null) {
            clientRepository.findById(s.getClientId()).ifPresent(c -> {
                String full = joinName(c.getLastName(), c.getFirstName(), c.getMiddleName());
                dto.setClientFullName(full);
                dto.setClientUserSub(c.getUserSub());
                dto.setClientEmail(c.getEmail());
            });
        }
        if (s.getCredit() != null) {
            creditRepository.findById(s.getCredit()).ifPresent(c -> {
                dto.setCreditAmount(c.getAmount());
                dto.setCreditTerm(c.getTerm());
                dto.setCreditRate(c.getRate());
                dto.setCreditMonthlyPayment(c.getMonthlyPayment());
                dto.setCreditStatus(c.getCreditStatus());
            });
        }
        return dto;
    }

    private static String joinName(String last, String first, String middle) {
        StringBuilder sb = new StringBuilder();
        if (last != null && !last.isBlank()) sb.append(last);
        if (first != null && !first.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(first);
        }
        if (middle != null && !middle.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(middle);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static ClientDto toClientDto(Client c) {
        return new ClientDto()
                .setLastName(c.getLastName())
                .setFirstName(c.getFirstName())
                .setMiddleName(c.getMiddleName())
                .setUserSub(c.getUserSub())
                .setBirthDate(c.getBirthDate())
                .setEmail(c.getEmail())
                .setGender(c.getGender())
                .setMaritalStatus(c.getMaritalStatus())
                .setDependentAmount(c.getDependentAmount())
                .setPassport(c.getPassport())
                .setEmployment(c.getEmployment())
                .setAccountNumber(c.getAccountNumber());
    }

    private static CreditResponseDto toCreditDto(Credit c) {
        return new CreditResponseDto()
                .setCreditId(c.getCreditId())
                .setAmount(c.getAmount())
                .setTerm(c.getTerm())
                .setMonthlyPayment(c.getMonthlyPayment())
                .setRate(c.getRate())
                .setPsk(c.getPsk())
                .setPaymentSchedule(c.getPaymentSchedule())
                .setIsInsuranceEnabled(c.getIsInsuranceEnabled())
                .setIsSalaryClient(c.getIsSalaryClient())
                .setCreditStatus(c.getCreditStatus());
    }

    // backwards-compatible (was used by the old /deal/admin/statement endpoint)
    public List<StatementDto> getAllStatements() {
        List<StatementDto> dtos = new ArrayList<>();
        statementRepository.findAll().forEach(s ->
                dtos.add(statementConvertor.statementEntityToStatementDto(s)));
        return dtos;
    }
}
