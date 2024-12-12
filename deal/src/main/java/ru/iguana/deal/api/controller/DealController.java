package ru.iguana.deal.api.controller;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
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
public class DealController {
    private final StatementService statementService;

    private final SelectOfferService selectOfferService;

    private final CalculateCreditService calculateCreditService;

    @PostMapping("/deal/statement")
    public ResponseEntity<List<JsonNode>> getOffers(@RequestBody JsonNode json) {
        return statementService.getLoanOfferList(json);
    }
    @PostMapping("/deal/offer/select")
    public void selectOffer(@RequestBody JsonNode json){
        selectOfferService.selectLoanOffer(json);
    }

    @PostMapping("/deal/calculate/{statementId}")
    public void calculate(@RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto,
                            @PathVariable String statementId) {

        calculateCreditService.calculate(finishRegistrationRequestDto, statementId);
    }


}
