package ru.iguana.statement.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.iguana.statement.api.dto.LoanStatementRequestDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StatementServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private StatementService statementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getLoanOfferList_Success() {
        LoanStatementRequestDto requestDto = new LoanStatementRequestDto(
                new BigDecimal("50000"),
                12,
                "John",
                "Doe",
                "Middle",
                "john.doe@example.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "567890"
        );

        JsonNode mockResponse = mock(JsonNode.class);
        when(mockResponse.isArray()).thenReturn(true);
        when(mockResponse.spliterator()).thenReturn(Collections.singletonList(mock(JsonNode.class)).spliterator());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/deal/statement"))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(eq(requestDto))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(JsonNode.class))).thenReturn(Mono.just(mockResponse));

        Mono<List<JsonNode>> result = statementService.getLoanOfferList(requestDto);

        List<JsonNode> responseList = result.block();
        assertNotNull(responseList);
        assertEquals(1, responseList.size());

        verify(webClient, times(1)).post();
        verify(requestBodyUriSpec, times(1)).uri(eq("/deal/statement"));
        verify(requestBodySpec, times(1)).bodyValue(eq(requestDto));
        verify(responseSpec, times(1)).bodyToMono(eq(JsonNode.class));
    }

    @Test
    void getLoanOfferList_ErrorResponse() {
        LoanStatementRequestDto requestDto = new LoanStatementRequestDto(
                new BigDecimal("50000"),
                12,
                "John",
                "Doe",
                "Middle",
                "john.doe@example.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "567890"
        );

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/deal/statement"))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(eq(requestDto))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(JsonNode.class))).thenReturn(Mono.error(new IllegalStateException("Invalid response structure")));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            statementService.getLoanOfferList(requestDto).block();
        });

        assertEquals("Invalid response structure", exception.getMessage());

        verify(webClient, times(1)).post();
        verify(requestBodyUriSpec, times(1)).uri(eq("/deal/statement"));
        verify(requestBodySpec, times(1)).bodyValue(eq(requestDto));
        verify(responseSpec, times(1)).bodyToMono(eq(JsonNode.class));
    }
}