package ru.iguana.calculator.api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.calculator.api.dto.CreditDto;
import ru.iguana.calculator.api.dto.LoanOfferDto;
import ru.iguana.calculator.api.dto.LoanStatementRequestDto;
import ru.iguana.calculator.api.dto.ScoringDataDto;

import java.util.List;

@RestController
public class CalculatorController {

    public static final String OFFERS = "/calculator/offers";

    public static final String CALCULATION  = "/calculator/calc";

    @PostMapping(OFFERS)
    public List<LoanOfferDto> calculateLoanOffers(@RequestBody LoanStatementRequestDto requestDto){
        //TODO implementation
        return null;
    }

    @PostMapping(CALCULATION)
    public CreditDto calculateCredit(@RequestBody ScoringDataDto scoringDataDto){
        //TODO implementation
        return null;
    }
}
