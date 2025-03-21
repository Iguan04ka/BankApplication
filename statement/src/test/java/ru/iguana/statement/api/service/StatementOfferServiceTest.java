package ru.iguana.statement.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StatementOfferServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private StatementOfferService statementOfferService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void selectOffer_Success() {
        JsonNode mockRequest = mock(JsonNode.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/deal/offer/select"))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(eq(mockRequest))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        ResponseEntity<Void> response = statementOfferService.selectOffer(mockRequest);

        assertNotNull(response);
        assertEquals(ResponseEntity.ok().build(), response);

        verify(webClient, times(1)).post();
        verify(requestBodyUriSpec, times(1)).uri(eq("/deal/offer/select"));
        verify(requestBodySpec, times(1)).bodyValue(eq(mockRequest));
        verify(responseSpec, times(1)).bodyToMono(Void.class);
    }

    @Test
    void selectOffer_Error() {
        JsonNode mockRequest = mock(JsonNode.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/deal/offer/select"))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(eq(mockRequest))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.error(new RuntimeException("Error occurred")));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            statementOfferService.selectOffer(mockRequest);
        });

        assertEquals("Error occurred", exception.getMessage());

        verify(webClient, times(1)).post();
        verify(requestBodyUriSpec, times(1)).uri(eq("/deal/offer/select"));
        verify(requestBodySpec, times(1)).bodyValue(eq(mockRequest));
        verify(responseSpec, times(1)).bodyToMono(Void.class);
    }
}
