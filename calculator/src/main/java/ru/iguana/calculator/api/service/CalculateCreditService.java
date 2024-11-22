package ru.iguana.calculator.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.iguana.calculator.api.config.LoanProperties;
import ru.iguana.calculator.api.dto.CreditDto;
import ru.iguana.calculator.api.dto.PaymentScheduleElementDto;
import ru.iguana.calculator.api.dto.ScoringDataDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculateCreditService {

    private final LoanProperties loanProperties;

    public CreditDto calculateCredit(ScoringDataDto scoringDataDto){
        //TODO implementation
        return null;
    }

    private BigDecimal calculateFinalRate(ScoringDataDto scoringDataDto){
        //TODO implementation
        return null;
    }

    private BigDecimal calculatePSK(ScoringDataDto scoringDataDto){
        //TODO implementation
        return null;
    }

    private List<PaymentScheduleElementDto> calculatePaymentSchedule(ScoringDataDto scoringDataDto){
        //TODO implementation
        return null;
    }
    public BigDecimal calculateMonthlyPayment(BigDecimal amount, Integer term, BigDecimal rate) {
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
