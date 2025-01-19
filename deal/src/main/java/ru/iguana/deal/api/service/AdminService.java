package ru.iguana.deal.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.iguana.deal.api.convertor.StatementConvertor;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.repository.StatementRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final StatementRepository statementRepository;
    private final StatementConvertor statementConvertor;

    public StatementDto getStatement(String statementId) {
        log.info("Fetching statement with ID: {}", statementId);
        UUID statementIdUUID = UUID.fromString(statementId);
        Statement statement = statementRepository.findById(statementIdUUID)
                .orElseThrow(() -> {
                    log.error("Statement not found for ID: {}", statementId);
                    return new RuntimeException("Statement not found");
                });

        StatementDto statementDto = statementConvertor.statementEntityToStatementDto(statement);
        log.info("Fetched statement: {}", statementDto);
        return statementDto;
    }

    public List<StatementDto> getAllStatements() {
        log.info("Fetching all statements");
        List<Statement> statements = (List<Statement>) statementRepository.findAll();

        List<StatementDto> statementDtos = statements.stream()
                .map(statementConvertor::statementEntityToStatementDto)
                .toList();

        log.info("Fetched {} statements", statementDtos.size());
        return statementDtos;
    }
}
