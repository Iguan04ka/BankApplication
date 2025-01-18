package ru.iguana.deal.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.iguana.deal.api.convertor.StatementConvertor;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.repository.StatementRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final StatementRepository statementRepository;

    private final StatementConvertor statementConvertor;

    public StatementDto getStatement(String statementId){
        UUID statementIdUUID = UUID.fromString(statementId);
        Statement statement = statementRepository.findById(statementIdUUID).orElseThrow();

        return statementConvertor.statementEntityToStatementDto(statement);
    }

    public List<StatementDto> getAllStatements(){
        List<Statement> statements = (List<Statement>) statementRepository.findAll();

        return statements
                .stream()
                .map(statementConvertor::statementEntityToStatementDto)
                .toList();

    }
}
