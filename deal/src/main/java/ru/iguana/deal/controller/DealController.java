package ru.iguana.deal.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.deal.dto.ClientDto;
import ru.iguana.deal.dto.StatementDto;
import ru.iguana.deal.entity.Client;
import ru.iguana.deal.entity.Statement;
import ru.iguana.deal.mapper.ClientMapper;
import ru.iguana.deal.mapper.StatementMapper;
import ru.iguana.deal.repository.ClientRepository;
import ru.iguana.deal.repository.CreditRepository;
import ru.iguana.deal.repository.StatementRepository;

@RestController
@AllArgsConstructor
public class DealController {
    private final ClientMapper clientMapper;

    private final StatementMapper statementMapper;
    private final ClientRepository clientRepository;

    private final CreditRepository creditRepository;

    private final StatementRepository statementRepository;

    @PostMapping("/api/map-client")
    public ResponseEntity<ClientDto> mapClient(@RequestBody String json) {
        try {
            ClientDto clientDto = clientMapper.jsonToClientDto(json);

            Client clientEntity = clientMapper.clientDtoToClientEntity(clientDto);

            clientRepository.save(clientEntity);

            StatementDto statementDto = new StatementDto();

            statementDto.setClientId(clientEntity.getClientId());

            Statement statementEntity = statementMapper.statementDtoToStatementEntity(statementDto);

            statementRepository.save(statementEntity);

            //TODO вынести логику в сервис

            return ResponseEntity.ok(clientDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
