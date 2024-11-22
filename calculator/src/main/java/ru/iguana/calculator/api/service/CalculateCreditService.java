package ru.iguana.calculator.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.iguana.calculator.api.config.LoanProperties;
import ru.iguana.calculator.api.dto.CreditDto;
import ru.iguana.calculator.api.dto.ScoringDataDto;

@Service
@RequiredArgsConstructor
public class CalculateCreditService {

    private final LoanProperties loanProperties;

    public CreditDto calculateCredit(ScoringDataDto scoringDataDto){
        return null;
    }

}
