package ru.iguana.gateway.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class UserClientProxyService {

    private static final String USER_SUB_HEADER = "X-User-Sub";

    private final RestClient restClient;

    public UserClientProxyService(@Qualifier("dealRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public JsonNode getProfile() {
        log.info("Proxying GET /deal/client/me");
        return execute(() -> restClient.get()
                .uri("/deal/client/me")
                .header(USER_SUB_HEADER, getUserSub())
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode updateProfile(JsonNode update) {
        log.info("Proxying PATCH /deal/client/me");
        return execute(() -> restClient.patch()
                .uri("/deal/client/me")
                .header(USER_SUB_HEADER, getUserSub())
                .body(update)
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode getStatements() {
        log.info("Proxying GET /deal/client/me/statements");
        return execute(() -> restClient.get()
                .uri("/deal/client/me/statements")
                .header(USER_SUB_HEADER, getUserSub())
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode getStatement(String statementId) {
        log.info("Proxying GET /deal/client/me/statements/{}", statementId);
        return execute(() -> restClient.get()
                .uri("/deal/client/me/statements/{statementId}", statementId)
                .header(USER_SUB_HEADER, getUserSub())
                .retrieve()
                .body(JsonNode.class));
    }

    public void denyStatement(String statementId) {
        log.info("Proxying POST /deal/client/me/statements/{}/deny", statementId);
        execute(() -> {
            restClient.post()
                    .uri("/deal/client/me/statements/{statementId}/deny", statementId)
                    .header(USER_SUB_HEADER, getUserSub())
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    public JsonNode getCredits() {
        log.info("Proxying GET /deal/client/me/credits");
        return execute(() -> restClient.get()
                .uri("/deal/client/me/credits")
                .header(USER_SUB_HEADER, getUserSub())
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode getCredit(String creditId) {
        log.info("Proxying GET /deal/client/me/credits/{}", creditId);
        return execute(() -> restClient.get()
                .uri("/deal/client/me/credits/{creditId}", creditId)
                .header(USER_SUB_HEADER, getUserSub())
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode getOffersForStatement(String statementId, String amount, String term) {
        log.info("Proxying GET /deal/client/me/statements/{}/offers (amount={}, term={})", statementId, amount, term);
        return execute(() -> {
            var uriSpec = restClient.get();
            String uri = "/deal/client/me/statements/{statementId}/offers";
            boolean hasAmount = amount != null && !amount.isBlank();
            boolean hasTerm   = term   != null && !term.isBlank();
            if (hasAmount && hasTerm) {
                return uriSpec.uri(uri + "?amount={amount}&term={term}", statementId, amount, term)
                        .header(USER_SUB_HEADER, getUserSub()).retrieve().body(JsonNode.class);
            } else if (hasAmount) {
                return uriSpec.uri(uri + "?amount={amount}", statementId, amount)
                        .header(USER_SUB_HEADER, getUserSub()).retrieve().body(JsonNode.class);
            } else if (hasTerm) {
                return uriSpec.uri(uri + "?term={term}", statementId, term)
                        .header(USER_SUB_HEADER, getUserSub()).retrieve().body(JsonNode.class);
            } else {
                return uriSpec.uri(uri, statementId)
                        .header(USER_SUB_HEADER, getUserSub()).retrieve().body(JsonNode.class);
            }
        });
    }

    private String getUserSub() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private <T> T execute(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            log.error("Deal service returned error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString());
        }
    }
}
