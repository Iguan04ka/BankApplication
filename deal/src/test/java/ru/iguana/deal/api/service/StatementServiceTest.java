package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.iguana.deal.api.dto.ClientDto;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.api.mapper.ClientMapper;
import ru.iguana.deal.api.mapper.StatementMapper;

import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Statement;

import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class StatementServiceTest {
    //TODO fix
    @Mock
    private ClientMapper clientMapper;

    @Mock
    private StatementMapper statementMapper;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private StatementRepository statementRepository;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private StatementService statementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
    }

    @Test
    void testGetLoanOfferList_Success() {
        JsonNode json = mock(JsonNode.class);
        ClientDto clientDto = mock(ClientDto.class);
        Client clientEntity = new Client();
        clientEntity.setClientId(UUID.randomUUID());
        Statement statementEntity = new Statement();
        statementEntity.setStatementId(UUID.randomUUID());

        when(clientMapper.jsonToClientDto(any(JsonNode.class))).thenReturn(clientDto);
        when(clientMapper.clientDtoToClientEntity(clientDto)).thenReturn(clientEntity);
        when(statementMapper.statementDtoToStatementEntity(any(StatementDto.class))).thenReturn(statementEntity);

        when(clientRepository.save(any(Client.class))).thenReturn(clientEntity);
        when(statementRepository.save(any(Statement.class))).thenReturn(statementEntity);

        List<JsonNode> loanOffers = new ArrayList<>();
        JsonNode loanOffer = mock(JsonNode.class);
        loanOffers.add(loanOffer);

        // Мокаем цепочку вызовов WebClient
        when(webClient.post()).thenReturn((WebClient.RequestBodyUriSpec) requestBodySpec);
        when(((WebClient.RequestBodyUriSpec) requestBodySpec).uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(JsonNode.class))).thenThrow((Throwable) requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mock(JsonNode.class)));

        ResponseEntity<List<JsonNode>> response = statementService.getLoanOfferList(json);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        verify(clientRepository).save(clientEntity);
        verify(statementRepository).save(statementEntity);
    }

    @Test
    void testGetLoanOfferList_Failure_WhenApiReturnsError() {
        // Подготовка данных
        JsonNode json = mock(JsonNode.class);
        ClientDto clientDto = mock(ClientDto.class);
        Client clientEntity = new Client();
        clientEntity.setClientId(UUID.randomUUID());
        Statement statementEntity = new Statement();
        statementEntity.setStatementId(UUID.randomUUID());

        when(clientMapper.jsonToClientDto(any(JsonNode.class))).thenReturn(clientDto);
        when(clientMapper.clientDtoToClientEntity(clientDto)).thenReturn(clientEntity);
        when(statementMapper.statementDtoToStatementEntity(any(StatementDto.class))).thenReturn(statementEntity);

        when(clientRepository.save(any(Client.class))).thenReturn(clientEntity);
        when(statementRepository.save(any(Statement.class))).thenReturn(statementEntity);

        when(webClient.post()).thenReturn((WebClient.RequestBodyUriSpec) requestBodySpec);
        when(((WebClient.RequestBodyUriSpec) requestBodySpec).uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(JsonNode.class))).thenThrow((Throwable) requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenThrow(new RuntimeException("API error"));


        ResponseEntity<List<JsonNode>> response = statementService.getLoanOfferList(json);

        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
    }
}