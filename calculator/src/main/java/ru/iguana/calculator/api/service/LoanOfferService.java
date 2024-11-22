package ru.iguana.calculator.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.iguana.calculator.api.config.LoanProperties;
import ru.iguana.calculator.api.dto.LoanOfferDto;
import ru.iguana.calculator.api.dto.LoanStatementRequestDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class LoanOfferService {
    private final LoanProperties loanProperties;

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

        BigDecimal currentRate = loanProperties.getBaseRate();

        LoanOfferDto offer = new LoanOfferDto();

        offer.setStatementId(UUID.randomUUID());
        offer.setRequestedAmount(requestDto.getAmount());
        offer.setTerm(requestDto.getTerm());

        BigDecimal insurance = new BigDecimal("0.00");

        if (isSalaryClient){
            currentRate = currentRate.subtract(new BigDecimal("1.00"));
        }
        if (isInsuranceEnabled){
            currentRate = currentRate.subtract(new BigDecimal("3.00"));

            /*
            Формула расчета страховки: baseCost + ((amount / 1000) * term)
            */

            insurance = insurance.add(loanProperties.getBaseCostOfInsurance()
                                                 .add((requestDto.getAmount().divide(new BigDecimal("1000"), RoundingMode.HALF_UP))
                                                 .multiply(BigDecimal.valueOf(requestDto.getTerm()))));

        }

        offer.setRate(currentRate);
        offer.setMonthlyPayment(calculateMonthlyPayment(requestDto.getAmount(),
                                                        requestDto.getTerm(),
                                                        currentRate));
        offer.setTotalAmount(insurance.add(offer.getMonthlyPayment()
                                  .multiply(BigDecimal.valueOf(requestDto.getTerm()))));
        offer.setIsSalaryClient(isSalaryClient);
        offer.setIsInsuranceEnabled(isInsuranceEnabled);

        return offer;
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal amount, Integer term, BigDecimal rate) {
        int scale = 10;
        /*
       Формула: amount * ( (monthlyRate * (1 + monthlyRate)^term )) / (1 + monthlyRate)^term - 1 )
        */

        //рассчет месячной процентной ставки: годовая ставка / 12
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12), scale, RoundingMode.HALF_UP);

        //преобразую процентную ставку в десятичный формат
        monthlyRate = monthlyRate.divide(BigDecimal.valueOf(100), scale, RoundingMode.HALF_UP);

        //(1 + monthlyRate) ^ term
        BigDecimal onePlusRatePowTerm = BigDecimal.ONE.add(monthlyRate).pow(term, new MathContext(scale, RoundingMode.HALF_UP));

        //monthlyRate * (1 + monthlyRate) ^ term
        BigDecimal numerator = monthlyRate.multiply(onePlusRatePowTerm);

        //(1 + monthlyRate) ^ term - 1
        BigDecimal denominator = onePlusRatePowTerm.subtract(BigDecimal.ONE);

        //numerator / denominator
        BigDecimal annuityCoefficient = numerator.divide(denominator, scale, RoundingMode.HALF_UP);

        //Округляем результат до 6 знаков после запятой
        annuityCoefficient = annuityCoefficient.setScale(6, RoundingMode.HALF_UP);

        return amount.multiply(annuityCoefficient);
    }
}
