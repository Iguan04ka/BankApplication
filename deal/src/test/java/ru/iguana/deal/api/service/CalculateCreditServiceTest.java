package ru.iguana.deal.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.iguana.deal.api.dto.FinishRegistrationRequestDto;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Jsonb.Passport;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.repository.ClientRepository;

import ru.iguana.deal.model.repository.StatementRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class CalculateCreditServiceTest {
    @Mock
    private StatementRepository statementRepository;
    @Mock
    private ClientRepository clientRepository;
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
        client.setBirthDate(LocalDate.of(1980,1,1));
        client.setPassport(new Passport()
                .setSeries("1234")
                .setSeries("123456"));

        finishRegistrationRequestDto = new FinishRegistrationRequestDto();
        finishRegistrationRequestDto.setGender("MALE");
        finishRegistrationRequestDto.setMaritalStatus("SINGLE");
        finishRegistrationRequestDto.setDependentAmount(0);
        finishRegistrationRequestDto.setPassportIssueDate(LocalDate.of(2015,1,1));
        finishRegistrationRequestDto.setPassportIssueBranch("Department 123");
        finishRegistrationRequestDto.setAccountNumber("1234567890");

    }

    @Test
    void calculate_statementNotFound() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> calculateCreditService.calculate(finishRegistrationRequestDto, statementId.toString())
        );
        assertEquals("No such id", exception.getMessage());
    }

    @Test
    void calculate_clientNotFound() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> calculateCreditService.calculate(finishRegistrationRequestDto, statementId.toString())
        );
        assertEquals("No such id", exception.getMessage());
    }
}
