package ru.iguana.deal.api.convertor;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.repository.StatementRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StatementConvertor {
    public Statement statementDtoToStatementEntity(StatementDto statementDto){
        Statement entity = new Statement();
        entity.setClientId(statementDto.getClientId());

        entity.getStatusHistory().addAll(statementDto.getStatusHistory());

        entity.setStatus(statementDto.getStatus());

        return entity;

    }
    public StatementDto statementEntityToStatementDto(Statement statement){
        StatementDto statementDto = new StatementDto();
        return statementDto
                .setClientId(statement.getClientId())
                .setCredit(statement.getCredit())
                .setStatus(statement.getStatus())
                .setCreationDate(statement.getCreationDate())
                .setAppliedOffer(statement.getAppliedOffer())
                .setSignDate(statement.getSignDate())
                .setSesCode(statement.getSesCode())
                .setStatusHistory(statement.getStatusHistory());
    }
}













