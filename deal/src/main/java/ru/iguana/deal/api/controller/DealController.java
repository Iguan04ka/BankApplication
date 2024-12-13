package ru.iguana.deal.api.controller;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.deal.api.dto.FinishRegistrationRequestDto;
import ru.iguana.deal.api.service.CalculateCreditService;
import ru.iguana.deal.api.service.SelectOfferService;
import ru.iguana.deal.api.service.StatementService;


import java.util.List;


@RestController
@AllArgsConstructor
@Slf4j
public class DealController {

    private final StatementService statementService;
    private final SelectOfferService selectOfferService;
    private final CalculateCreditService calculateCreditService;

    @PostMapping("/deal/statement")
    public ResponseEntity<List<JsonNode>> getOffers(@RequestBody JsonNode json) {
        log.info("Received request to get loan offers");
        log.debug("Received request to get loan offers: {}", json);
        ResponseEntity<List<JsonNode>> response = statementService.getLoanOfferList(json);
        log.info("Loan offers successfully retrieved:");
        log.debug("Loan offers successfully retrieved: {}", response);
        return response;
    }

    @PostMapping("/deal/offer/select")
    public void selectOffer(@RequestBody JsonNode json) {
        log.info("Received request to select loan offer");
        log.debug("Received request to select loan offer: {}", json);
        try {
            selectOfferService.selectLoanOffer(json);
            log.info("Loan offer successfully selected.");
        } catch (Exception e) {
            log.error("Error selecting loan offer: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/deal/calculate/{statementId}")
    public void calculate(@RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto,
                          @PathVariable String statementId) {
        log.info("Received request to calculate credit for statementId");
        log.debug("Received request to calculate credit for statementId: {} with data: {}", statementId, finishRegistrationRequestDto);
        try {
            calculateCreditService.calculate(finishRegistrationRequestDto, statementId);
            log.info("Credit successfully calculated for statementId: {}", statementId);
        } catch (Exception e) {
            log.error("Error calculating credit for statementId: {}", statementId, e);
            throw e;
        }
    }
}
