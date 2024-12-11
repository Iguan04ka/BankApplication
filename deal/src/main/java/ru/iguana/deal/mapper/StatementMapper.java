package ru.iguana.deal.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.iguana.deal.dto.ClientDto;
import ru.iguana.deal.dto.StatementDto;
import ru.iguana.deal.entity.Client;
import ru.iguana.deal.entity.Statement;

@Component
@AllArgsConstructor
public class StatementMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Statement statementDtoToStatementEntity(StatementDto statementDto){
        Statement entity = new Statement();
        entity.setClientId(statementDto.getClientId());

        return entity;

    }
}
