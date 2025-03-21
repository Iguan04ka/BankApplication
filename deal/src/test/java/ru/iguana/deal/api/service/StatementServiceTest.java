package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.iguana.deal.api.config.DealProperties;
import ru.iguana.deal.api.convertor.ClientConvertor;
import ru.iguana.deal.api.convertor.StatementConvertor;
import ru.iguana.deal.api.dto.ClientDto;
import ru.iguana.deal.api.dto.StatementDto;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StatementServiceTest {

    @InjectMocks
    private StatementService statementService;

    @Mock
    private ClientConvertor clientConvertor;

    @Mock
    private ClientRepository clientRepository;


    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetLoanOfferList_Failure() {
        // Mock input data
        JsonNode inputJson = objectMapper.createObjectNode();

        // Mock dependencies to throw an exception
        when(clientConvertor.jsonToClientDto(inputJson)).thenThrow(new RuntimeException("Test exception"));

        // Call the method under test
        ResponseEntity<List<JsonNode>> response = statementService.getLoanOfferList(inputJson);

        // Verify
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void testClientIsSavedToDatabase() {
        // Mock input data
        JsonNode inputJson = objectMapper.createObjectNode();
        ClientDto clientDto = new ClientDto();
        Client clientEntity = new Client();
        clientEntity.setClientId(UUID.randomUUID());

        // Mock dependencies
        when(clientConvertor.jsonToClientDto(inputJson)).thenReturn(clientDto);
        when(clientConvertor.clientDtoToClientEntity(clientDto)).thenReturn(clientEntity);
        when(clientRepository.save(any(Client.class))).thenReturn(clientEntity);

        // Call the method under test
        statementService.getLoanOfferList(inputJson);

        // Verify
        verify(clientRepository, times(1)).save(clientEntity);
    }
}