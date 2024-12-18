package ru.iguana.deal.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import ru.iguana.deal.api.dto.FinishRegistrationRequestDto;

import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Jsonb.Passport;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.repository.ClientRepository;

import ru.iguana.deal.model.repository.StatementRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculateCreditServiceTest {
//TODO fix
    @Mock
    private StatementRepository statementRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.Builder webClientBuilder;
    @InjectMocks
    private CalculateCreditService calculateCreditService;

    private UUID statementId;
    private Statement statement;
    private Client client;
    private FinishRegistrationRequestDto finishRegistrationRequestDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        statementId = UUID.randomUUID();
        statement = new Statement();
        statement.setStatementId(statementId);
        statement.setClientId(UUID.randomUUID());

        client = new Client();
        client.setClientId(statement.getClientId());
        client.setFirstName("John");
        client.setLastName("Doe");
        client.setMiddleName("Middle");
        client.setBirthDate(LocalDate.from(Instant.parse("1980-01-01T00:00:00Z")));
        client.setPassport(new Passport()
                .setSeries("1234")
                .setSeries("123456"));

        finishRegistrationRequestDto = new FinishRegistrationRequestDto();
        finishRegistrationRequestDto.setGender("MALE");
        finishRegistrationRequestDto.setMaritalStatus("SINGLE");
        finishRegistrationRequestDto.setDependentAmount(0);
        finishRegistrationRequestDto.setPassportIssueDate(LocalDate.from(Instant.parse("2015-01-01T00:00:00Z")));
        finishRegistrationRequestDto.setPassportIssueBranch("Department 123");
        finishRegistrationRequestDto.setAccountNumber("1234567890");

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
    }

    @Test
    void calculate_statementNotFound() {

        when(statementRepository.findById(statementId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> calculateCreditService.calculate(finishRegistrationRequestDto, statementId.toString())
        );
        assertEquals("No such id", exception.getMessage());
    }

    @Test
    void calculate_clientNotFound() {
        // Arrange
        when(statementRepository.findById(statementId)).thenReturn(Optional.of(statement));
        when(clientRepository.findById(statement.getClientId())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> calculateCreditService.calculate(finishRegistrationRequestDto, statementId.toString())
        );
        assertEquals("No such id", exception.getMessage());
    }
}