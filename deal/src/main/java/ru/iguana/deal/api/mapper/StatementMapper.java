package ru.iguana.deal.api.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.model.entity.Statement;

@Component
@AllArgsConstructor
public class StatementMapper {
    public Statement statementDtoToStatementEntity(StatementDto statementDto){
        Statement entity = new Statement();
        entity.setClientId(statementDto.getClientId());

        entity.getStatusHistory().addAll(statementDto.getStatusHistory()); //Todo заглушка

        entity.setStatus(statementDto.getStatus());

        return entity;

    }
}
