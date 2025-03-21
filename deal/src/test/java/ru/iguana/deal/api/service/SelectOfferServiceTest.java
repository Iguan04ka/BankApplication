package ru.iguana.deal.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.iguana.deal.model.entity.Jsonb.StatusHistory;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.ApplicationStatus;
import ru.iguana.deal.model.entity.enums.ChangeType;
import ru.iguana.deal.model.repository.StatementRepository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelectOfferServiceTest {

    @Mock
    private StatementRepository statementRepository;

    @InjectMocks
    private SelectOfferService selectOfferService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void selectLoanOffer_shouldUpdateStatementWithOfferAndStatus() throws Exception {
        UUID statementId = UUID.randomUUID();
        Statement mockStatement = new Statement();
        mockStatement.setStatusHistory(new ArrayList<>());

        JsonNode requestJson = objectMapper.readTree("{\"statementId\": \"" + statementId + "\", \"offer\": \"details\"}");

        when(statementRepository.findById(statementId)).thenReturn(Optional.of(mockStatement));

        doAnswer(invocation -> {
            Statement statement = invocation.getArgument(0);

            assertEquals(ApplicationStatus.APPROVED.name(), statement.getStatus());

            assertEquals(requestJson, statement.getAppliedOffer());

            assertEquals(1, statement.getStatusHistory().size());
            StatusHistory statusHistory = statement.getStatusHistory().get(0);
            assertEquals(ApplicationStatus.APPROVED, statusHistory.getStatus());
            assertEquals(ChangeType.AUTOMATIC, statusHistory.getChangeType());
            return null;
        }).when(statementRepository).save(any());

        selectOfferService.selectLoanOffer(requestJson);

        verify(statementRepository).findById(statementId);
        verify(statementRepository).save(mockStatement);
    }

    @Test
    void selectLoanOffer_shouldThrowExceptionWhenStatementNotFound() throws Exception {

        UUID statementId = UUID.randomUUID();
        JsonNode requestJson = objectMapper.readTree("{\"statementId\": \"" + statementId + "\", \"offer\": \"details\"}");

        when(statementRepository.findById(statementId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> selectOfferService.selectLoanOffer(requestJson)
        );
        assertEquals("No such statement", exception.getMessage());

        verify(statementRepository).findById(statementId);
        verify(statementRepository, never()).save(any());
    }

    @Test
    void selectLoanOffer_shouldThrowExceptionWhenJsonInvalid() throws Exception {
        JsonNode invalidJson = objectMapper.readTree("{\"invalidKey\": \"value\"}");


        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> selectOfferService.selectLoanOffer(invalidJson)
        );
        assertEquals("JSON does not contain key 'statementId'", exception.getMessage());

        verify(statementRepository, never()).findById(any());
        verify(statementRepository, never()).save(any());
    }
}
