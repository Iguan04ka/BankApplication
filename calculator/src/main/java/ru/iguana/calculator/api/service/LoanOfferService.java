package ru.iguana.calculator.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.iguana.calculator.api.dto.LoanOfferDto;
import ru.iguana.calculator.api.dto.LoanStatementRequestDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class LoanOfferService {
    private final CalculateCreditService calculateCreditService;

    public List<LoanOfferDto> getOffers(LoanStatementRequestDto requestDto){
        return List.of(
                calculateOffer(false, false, requestDto),
                calculateOffer(false, true, requestDto),
                calculateOffer(true, false, requestDto),
                calculateOffer(true, true, requestDto)
        );
    }

    private LoanOfferDto calculateOffer(Boolean isInsuranceEnabled,
                           Boolean isSalaryClient,
                           LoanStatementRequestDto requestDto){

        LoanOfferDto offer = new LoanOfferDto();

        offer.setStatementId(UUID.randomUUID());
        offer.setTerm(requestDto.getTerm());

        BigDecimal finalRate = calculateCreditService.calculateFinalRate(isSalaryClient, isInsuranceEnabled);
        offer.setRate(finalRate);

        BigDecimal insurance = new BigDecimal("0");

        if(isInsuranceEnabled){
            insurance = insurance.add(calculateCreditService.calculateInsurance(
                    requestDto.getAmount(), requestDto.getTerm()));
        }

        BigDecimal requestedAmountWithInsurance = requestDto.getAmount().add(insurance);
        offer.setRequestedAmount(requestedAmountWithInsurance);

        offer.setMonthlyPayment(calculateCreditService.calculateMonthlyPaymentAnnuity(
                                                        requestedAmountWithInsurance,
                                                        requestDto.getTerm(),
                                                        finalRate));
        offer.setTotalAmount(offer.getMonthlyPayment().multiply(BigDecimal.valueOf(requestDto.getTerm())));

        offer.setIsSalaryClient(isSalaryClient);
        offer.setIsInsuranceEnabled(isInsuranceEnabled);

        return offer;
    }


}
