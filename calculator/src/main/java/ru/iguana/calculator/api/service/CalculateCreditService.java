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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculateCreditService {

    private final LoanProperties loanProperties;

    public CreditDto calculateCredit(ScoringDataDto scoringDataDto){
        CreditDto creditDto = new CreditDto();

        if (scoringDataDto.getIsInsuranceEnabled()){
            creditDto.setAmount(scoringDataDto.getAmount().add(calculateInsurance(
                    scoringDataDto.getAmount(), scoringDataDto.getTerm()
            )));
        }
        else creditDto.setAmount(scoringDataDto.getAmount());

        creditDto.setTerm(scoringDataDto.getTerm());

        creditDto.setMonthlyPayment(calculateMonthlyPaymentAnnuity(
                scoringDataDto.getAmount(),
                scoringDataDto.getTerm(),
                calculateFinalRate(scoringDataDto)
        ));

        creditDto.setRate(calculateFinalRate(scoringDataDto));

        creditDto.setPsk(calculatePSK(scoringDataDto));

        creditDto.setIsInsuranceEnabled(scoringDataDto.getIsInsuranceEnabled());

        creditDto.setIsSalaryClient(scoringDataDto.getIsSalaryClient());

        creditDto.setPaymentSchedule(calculatePaymentSchedule(scoringDataDto));

        return creditDto;
    }

    private BigDecimal calculateFinalRate(ScoringDataDto scoringDataDto){
        BigDecimal finalRate = loanProperties.getBaseRate();

        // Проверка: Сумма займа больше 24 зарплат
        if (scoringDataDto.getAmount()
                .compareTo(scoringDataDto.getEmployment().getSalary().multiply(new BigDecimal("24"))) > 0) {
            throw new IllegalArgumentException("the loan amount is too large");
        }

        // Проверка: Возраст
        if (getAge(scoringDataDto.getBirthdate()) < 20 || getAge(scoringDataDto.getBirthdate()) > 65){
            throw new IllegalArgumentException("incorrect age");
        }

        // Проверка: Стаж работы
        if (scoringDataDto.getEmployment().getWorkExperienceTotal() < 18 ||
            scoringDataDto.getEmployment().getWorkExperienceCurrent() < 3){

            throw new IllegalArgumentException("insufficient work experience");
        }

        //Проверка: зарплатный клиент
        if(scoringDataDto.getIsSalaryClient()){
            finalRate = finalRate.subtract(new BigDecimal("1"));
        }

        //Проверка: на кредит оформлена страховка
        if (scoringDataDto.getIsInsuranceEnabled()){
            finalRate = finalRate.subtract(new BigDecimal("3"));
        }

        // Проверка: Рабочий статус
        switch (scoringDataDto.getEmployment().getEmploymentStatus()){
            case SELFEMPLOYED -> finalRate = finalRate.add(new BigDecimal("2"));
            case HIREDEMPLOYED -> finalRate = finalRate.add(new BigDecimal("1"));
            case UNEMPLOYED -> throw new IllegalArgumentException("We do not provide loans to the unemployed");
            default -> throw new IllegalArgumentException("Invalid operating status specified");
        }

        // Проверка: Позиция на работе
        switch (scoringDataDto.getEmployment().getPosition()){
            case JUNIOR -> finalRate = finalRate.add(new BigDecimal("3"));
            case MIDDLE -> finalRate = finalRate.add(new BigDecimal("2"));
            case SENIOR -> finalRate = finalRate.add(new BigDecimal("1"));
            case BOSS -> finalRate = finalRate.add(new BigDecimal("1"));
            default -> throw new IllegalArgumentException("Incorrect job position indicated");
        }

        // Проверка: Семейное положение
        switch (scoringDataDto.getMaritalStatus()){
            case MARRIED -> finalRate = finalRate.subtract(new BigDecimal("3"));
            case DIVORCED -> finalRate = finalRate.add(new BigDecimal("1"));
            default -> throw new IllegalArgumentException("marital status is indicated incorrectly");
        }

        // Проверка: Пол и возраст
        switch (scoringDataDto.getGender()){
            case FEMALE -> {
                if (getAge(scoringDataDto.getBirthdate()) >= 32 && getAge(scoringDataDto.getBirthdate()) <= 60){
                    finalRate = finalRate.subtract(new BigDecimal("3"));
                }
            }
            case MALE -> {
                if (getAge(scoringDataDto.getBirthdate()) >= 30 && getAge(scoringDataDto.getBirthdate()) <= 55){
                    finalRate = finalRate.subtract(new BigDecimal("3"));
                }
            }
            case NONBINARY -> finalRate = finalRate.add(new BigDecimal("7"));
        }

        return finalRate;
    }

    private BigDecimal calculatePSK(ScoringDataDto scoringDataDto){
        /*
        Формула ПСК: ПСК = (СП/СЗ – 1) / C * 100,

        где СП – сумма всех платежей клиента;
        СЗ – сумма выданного потребительского кредита;
        С – срок кредитования в годах. */

        BigDecimal amountOfPayments = calculateMonthlyPaymentAnnuity(scoringDataDto.getAmount(),
                                                              scoringDataDto.getTerm(),
                                                              calculateFinalRate(scoringDataDto))
                   .multiply(BigDecimal.valueOf(scoringDataDto.getTerm()));

        BigDecimal loanTermInYears = BigDecimal.valueOf(scoringDataDto.getTerm()).divide(new BigDecimal("12"), RoundingMode.HALF_UP);

        return (((amountOfPayments.divide(scoringDataDto.getAmount(), RoundingMode.HALF_UP)).subtract(new BigDecimal("1")))
                        .divide(loanTermInYears, RoundingMode.HALF_UP)).multiply(new BigDecimal("100"));

    }

    private List<PaymentScheduleElementDto> calculatePaymentSchedule(ScoringDataDto scoringDataDto){
        List<PaymentScheduleElementDto> payments = new ArrayList<>();

        BigDecimal principal = scoringDataDto.getAmount();

        BigDecimal remainingDebt = principal;

        // Начальная дата — сегодняшняя
        LocalDate startDate = LocalDate.now();

        for (int i = 1; i <= scoringDataDto.getTerm(); i++) {
            LocalDate paymentDate = startDate.plusMonths(i - 1);

            // Используем метод для расчета одного платежа
            BigDecimal[] paymentDetails = calculateMonthlyPaymentDifferentiate(
                    remainingDebt, principal, calculateFinalRate(scoringDataDto), scoringDataDto.getTerm(), paymentDate);

            BigDecimal totalPayment = paymentDetails[0];
            BigDecimal interestPayment = paymentDetails[1];
            BigDecimal debtPayment = paymentDetails[2];

            // Обновляем остаток долга
            if (i == scoringDataDto.getTerm()) {
                debtPayment = remainingDebt; // Закрываем остаток долга
                totalPayment = debtPayment.add(interestPayment); // Общий платеж — долг + проценты
                remainingDebt = BigDecimal.ZERO; // Полное погашение
            } else {
                // Обновляем остаток долга
                remainingDebt = remainingDebt.subtract(debtPayment).setScale(2, RoundingMode.HALF_UP);
            }

            // Добавляем элемент в график
            payments.add(new PaymentScheduleElementDto(
                    i,
                    paymentDate,
                    totalPayment,
                    interestPayment,
                    debtPayment,
                    remainingDebt.max(BigDecimal.ZERO)
            ));
        }
        return payments;
    }

    private Long getAge(LocalDate birthdate){
        return Math.abs(ChronoUnit.YEARS.between(birthdate, LocalDate.now()));
    }

    public BigDecimal calculateMonthlyPaymentAnnuity(BigDecimal amount, Integer term, BigDecimal rate) {
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

    private BigDecimal[] calculateMonthlyPaymentDifferentiate(BigDecimal remainingDebt, BigDecimal principal,
                                                              BigDecimal annualRate, int months, LocalDate currentDate) {

        int daysInMonth = currentDate.lengthOfMonth();
        int daysInYear = currentDate.isLeapYear() ? 366 : 365;

        // считаем платёж по основному долгу
        BigDecimal debtPayment = principal.divide(BigDecimal.valueOf(months), 10, RoundingMode.HALF_UP);

        annualRate = annualRate.divide(new BigDecimal("100"), RoundingMode.HALF_UP);

        // считаем платёж по процентам
        BigDecimal interestPayment = remainingDebt.multiply(annualRate)
                .multiply(BigDecimal.valueOf(daysInMonth))
                .divide(BigDecimal.valueOf(daysInYear), 10, RoundingMode.HALF_UP);

        // общий платёж
        BigDecimal totalPayment = debtPayment.add(interestPayment).setScale(2, RoundingMode.HALF_UP);

        //[0] — общая сумма платежа, [1] — платеж по процентам, [2] — платеж по основному долгу
        return new BigDecimal[]{
                totalPayment,
                interestPayment.setScale(2, RoundingMode.HALF_UP),
                debtPayment.setScale(2, RoundingMode.HALF_UP)
        };
    }

    public BigDecimal calculateInsurance(BigDecimal amount, Integer term){
        /*
        Формула расчета страховки: baseCost + ((amount / 1000) * term)
        */
        return loanProperties.getBaseCostOfInsurance()
                             .add((amount.divide(new BigDecimal("1000"), RoundingMode.HALF_UP))
                             .multiply(BigDecimal.valueOf(term)));
    }


}
