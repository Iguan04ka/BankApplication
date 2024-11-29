package ru.iguana.calculator.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculateCreditService {

    private final LoanProperties loanProperties;

    public CreditDto calculateCredit(ScoringDataDto scoringDataDto){
        log.info("Starting credit calculation for request: {}", scoringDataDto);

        try {
            CreditDto creditDto = new CreditDto();

            if (scoringDataDto.getIsInsuranceEnabled()) {
                BigDecimal insurance = calculateInsurance(scoringDataDto.getAmount(), scoringDataDto.getTerm());
                log.debug("Calculated insurance: {}", insurance);
                creditDto.setAmount(scoringDataDto.getAmount().add(insurance));
            } else {
                creditDto.setAmount(scoringDataDto.getAmount());
            }

            creditDto.setTerm(scoringDataDto.getTerm());

            BigDecimal finalRate = calculateFinalRate(scoringDataDto);
            log.debug("Calculated final rate: {}", finalRate);

            BigDecimal monthlyPayment = calculateMonthlyPaymentAnnuity(
                    scoringDataDto.getAmount(),
                    scoringDataDto.getTerm(),
                    finalRate
            );

            creditDto.setMonthlyPayment(monthlyPayment);
            log.debug("Calculated monthly payment: {}", monthlyPayment);

            BigDecimal psk = calculatePSK(scoringDataDto);
            creditDto.setPsk(psk);
            log.debug("Calculated PSK: {}", psk);

            creditDto.setRate(finalRate);
            creditDto.setIsInsuranceEnabled(scoringDataDto.getIsInsuranceEnabled());
            creditDto.setIsSalaryClient(scoringDataDto.getIsSalaryClient());

            List<PaymentScheduleElementDto> paymentSchedule = calculatePaymentSchedule(scoringDataDto);
            creditDto.setPaymentSchedule(paymentSchedule);
            log.debug("Generated payment schedule: {}", paymentSchedule);

            log.info("Successfully calculated credit: {}", creditDto);
            return creditDto;

        } catch (Exception e) {
            log.error("Error occurred during credit calculation for request: {}", scoringDataDto, e);
            throw e;
        }
    }

    private BigDecimal calculateFinalRate(ScoringDataDto scoringDataDto) {
        log.info("Calculating final rate. Scoring data: {}", scoringDataDto);

        BigDecimal finalRate = loanProperties.getBaseRate();
        log.debug("Base rate: {}", finalRate);

        // Проверка: Сумма займа больше 24 зарплат
        if (scoringDataDto.getAmount()
                .compareTo(scoringDataDto.getEmployment().getSalary().multiply(new BigDecimal("24"))) > 0) {
            log.error("Loan amount is too large: {} > 24 * Salary {}", scoringDataDto.getAmount(), scoringDataDto.getEmployment().getSalary());
            throw new IllegalArgumentException("the loan amount is too large");
        }

        // Проверка: Возраст
        long age = getAge(scoringDataDto.getBirthdate());
        if (age < 20 || age > 65) {
            log.error("Incorrect age: {}. Allowed range: 20-65.", age);
            throw new IllegalArgumentException("incorrect age");
        }
        log.debug("Age: {}", age);

        // Проверка: Стаж работы
        if (scoringDataDto.getEmployment().getWorkExperienceTotal() < 18 ||
                scoringDataDto.getEmployment().getWorkExperienceCurrent() < 3) {
            log.error("Insufficient work experience. Total: {}, Current: {}",
                    scoringDataDto.getEmployment().getWorkExperienceTotal(),
                    scoringDataDto.getEmployment().getWorkExperienceCurrent());
            throw new IllegalArgumentException("insufficient work experience");
        }

        // Проверка: зарплатный клиент
        if (scoringDataDto.getIsSalaryClient()) {
            log.debug("Salary client detected. Subtracting 1 from rate.");
            finalRate = finalRate.subtract(new BigDecimal("1"));
        }

        // Проверка: на кредит оформлена страховка
        if (scoringDataDto.getIsInsuranceEnabled()) {
            log.debug("Insurance enabled. Subtracting 3 from rate.");
            finalRate = finalRate.subtract(new BigDecimal("3"));
        }

        // Проверка: Рабочий статус
        switch (scoringDataDto.getEmployment().getEmploymentStatus()) {
            case SELFEMPLOYED -> {
                log.debug("Employment status: SELFEMPLOYED. Adding 2 to rate.");
                finalRate = finalRate.add(new BigDecimal("2"));
            }
            case HIREDEMPLOYED -> {
                log.debug("Employment status: HIREDEMPLOYED. Adding 1 to rate.");
                finalRate = finalRate.add(new BigDecimal("1"));
            }
            case UNEMPLOYED -> {
                log.error("Employment status: UNEMPLOYED. Loan denied.");
                throw new IllegalArgumentException("We do not provide loans to the unemployed");
            }
            default -> {
                log.error("Invalid employment status: {}", scoringDataDto.getEmployment().getEmploymentStatus());
                throw new IllegalArgumentException("Invalid operating status specified");
            }
        }

        // Проверка: Позиция на работе
        switch (scoringDataDto.getEmployment().getPosition()) {
            case JUNIOR -> {
                log.debug("Position: JUNIOR. Adding 3 to rate.");
                finalRate = finalRate.add(new BigDecimal("3"));
            }
            case MIDDLE -> {
                log.debug("Position: MIDDLE. Adding 2 to rate.");
                finalRate = finalRate.add(new BigDecimal("2"));
            }
            case SENIOR, BOSS -> {
                log.debug("Position: {}. Adding 1 to rate.", scoringDataDto.getEmployment().getPosition());
                finalRate = finalRate.add(new BigDecimal("1"));
            }
            default -> {
                log.error("Invalid job position: {}", scoringDataDto.getEmployment().getPosition());
                throw new IllegalArgumentException("Incorrect job position indicated");
            }
        }

        // Проверка: Семейное положение
        switch (scoringDataDto.getMaritalStatus()) {
            case MARRIED -> {
                log.debug("Marital status: MARRIED. Subtracting 3 from rate.");
                finalRate = finalRate.subtract(new BigDecimal("3"));
            }
            case DIVORCED -> {
                log.debug("Marital status: DIVORCED. Adding 1 to rate.");
                finalRate = finalRate.add(new BigDecimal("1"));
            }
            default -> {
                log.error("Invalid marital status: {}", scoringDataDto.getMaritalStatus());
                throw new IllegalArgumentException("marital status is indicated incorrectly");
            }
        }

        // Проверка: Пол и возраст
        switch (scoringDataDto.getGender()) {
            case FEMALE -> {
                if (age >= 32 && age <= 60) {
                    log.debug("Gender: FEMALE, age in range 32-60. Subtracting 3 from rate.");
                    finalRate = finalRate.subtract(new BigDecimal("3"));
                }
            }
            case MALE -> {
                if (age >= 30 && age <= 55) {
                    log.debug("Gender: MALE, age in range 30-55. Subtracting 3 from rate.");
                    finalRate = finalRate.subtract(new BigDecimal("3"));
                }
            }
            case NONBINARY -> {
                log.debug("Gender: NONBINARY. Adding 7 to rate.");
                finalRate = finalRate.add(new BigDecimal("7"));
            }
        }

        log.info("Final rate calculation completed. Final rate: {}", finalRate);
        return finalRate;
    }

    public BigDecimal calculateFinalRate(Boolean isSalaryClient, Boolean isInsuranceEnabled) {
        log.info("Calculating final rate. isSalaryClient: {}, isInsuranceEnabled: {}", isSalaryClient, isInsuranceEnabled);

        BigDecimal currentRate = loanProperties.getBaseRate();
        log.debug("Base rate: {}", currentRate);

        if (isSalaryClient) {
            log.debug("Salary client detected. Subtracting 1 from rate.");
            currentRate = currentRate.subtract(new BigDecimal("1"));
        }

        if (isInsuranceEnabled) {
            log.debug("Insurance enabled. Subtracting 3 from rate.");
            currentRate = currentRate.subtract(new BigDecimal("3"));
        }

        log.info("Final rate calculation completed. Final rate: {}", currentRate);
        return currentRate;
    }

    private BigDecimal calculatePSK(ScoringDataDto scoringDataDto){
        /*
        Формула ПСК: ПСК = (СП/СЗ – 1) / C * 100,

        где СП – сумма всех платежей клиента;
        СЗ – сумма выданного потребительского кредита;
        С – срок кредитования в годах. */

        log.info("Calculating PSK for scoring data: {}", scoringDataDto);

        try {
            BigDecimal amountOfPayments = calculateMonthlyPaymentAnnuity(
                    scoringDataDto.getAmount(),
                    scoringDataDto.getTerm(),
                    calculateFinalRate(scoringDataDto)
            ).multiply(BigDecimal.valueOf(scoringDataDto.getTerm()));

            BigDecimal loanTermInYears = BigDecimal.valueOf(scoringDataDto.getTerm()).divide(new BigDecimal("12"), RoundingMode.HALF_UP);

            BigDecimal psk = (((amountOfPayments.divide(scoringDataDto.getAmount(), RoundingMode.HALF_UP)).subtract(new BigDecimal("1")))
                    .divide(loanTermInYears, RoundingMode.HALF_UP)).multiply(new BigDecimal("100"));

            log.info("PSK calculated: {}", psk);
            return psk;

        } catch (Exception e) {
            log.error("Error calculating PSK for scoring data: {}", scoringDataDto, e);
            throw e;
        }
    }

    private List<PaymentScheduleElementDto> calculatePaymentSchedule(ScoringDataDto scoringDataDto) {
        log.info("Calculating payment schedule for loan amount: {}, term: {}", scoringDataDto.getAmount(), scoringDataDto.getTerm());
        List<PaymentScheduleElementDto> payments = new ArrayList<>();

        BigDecimal principal = scoringDataDto.getAmount();
        BigDecimal remainingDebt = principal;

        // Начальная дата — сегодняшняя
        LocalDate startDate = LocalDate.now();
        log.debug("Starting date for payments: {}", startDate);

        for (int i = 1; i <= scoringDataDto.getTerm(); i++) {
            LocalDate paymentDate = startDate.plusMonths(i - 1);
            log.debug("Calculating payment for month {}: payment date: {}", i, paymentDate);

            // Используем метод для расчета одного платежа
            BigDecimal[] paymentDetails = calculateMonthlyPaymentAnnuity(
                    remainingDebt, principal, calculateFinalRate(scoringDataDto), scoringDataDto.getTerm(), paymentDate);

            BigDecimal totalPayment = paymentDetails[0];
            BigDecimal interestPayment = paymentDetails[1];
            BigDecimal debtPayment = paymentDetails[2];

            // Логирование значений для каждого платежа
            log.debug("Payment details for month {}: Total payment: {}, Interest payment: {}, Debt payment: {}",
                    i, totalPayment, interestPayment, debtPayment);

            // Обновляем остаток долга
            if (i == scoringDataDto.getTerm()) {
                debtPayment = remainingDebt; // Закрываем остаток долга
                totalPayment = debtPayment.add(interestPayment); // Общий платеж — долг + проценты
                remainingDebt = BigDecimal.ZERO; // Полное погашение
                log.debug("Final payment. Remaining debt: {}, Total payment: {}", remainingDebt, totalPayment);
            } else {
                // Обновляем остаток долга
                remainingDebt = remainingDebt.subtract(debtPayment).setScale(2, RoundingMode.HALF_UP);
                log.debug("Remaining debt after month {}: {}", i, remainingDebt);
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

        log.info("Payment schedule calculation completed. Total payments count: {}", payments.size());
        return payments;
    }

    private Long getAge(LocalDate birthdate){
        Long age = Math.abs(ChronoUnit.YEARS.between(birthdate, LocalDate.now()));

        log.debug("Calculated age: {} for birthdate: {}", age, birthdate);

        return age;
    }

    public BigDecimal calculateMonthlyPaymentAnnuity(BigDecimal amount, Integer term, BigDecimal rate) {
        log.info("Calculating monthly payment. Amount: {}, Term: {}, Rate: {}", amount, term, rate);

        try {
            BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

            BigDecimal onePlusRatePowTerm = BigDecimal.ONE.add(monthlyRate).pow(term, new MathContext(10, RoundingMode.HALF_UP));
            BigDecimal numerator = monthlyRate.multiply(onePlusRatePowTerm);
            BigDecimal denominator = onePlusRatePowTerm.subtract(BigDecimal.ONE);
            BigDecimal annuityCoefficient = numerator.divide(denominator, 10, RoundingMode.HALF_UP);

            BigDecimal monthlyPayment = amount.multiply(annuityCoefficient).setScale(2, RoundingMode.HALF_UP);

            log.info("Monthly payment calculated: {}", monthlyPayment);
            return monthlyPayment;

        } catch (Exception e) {
            log.error("Error calculating monthly payment. Amount: {}, Term: {}, Rate: {}", amount, term, rate, e);
            throw e;
        }
    }

    private BigDecimal[] calculateMonthlyPaymentAnnuity(BigDecimal remainingDebt, BigDecimal principal,
                                                        BigDecimal annualRate, int months, LocalDate currentDate) {
        log.info("Calculating monthly payment annuity. Remaining debt: {}, Principal: {}, Annual rate: {}, Months: {}, Current date: {}",
                remainingDebt, principal, annualRate, months, currentDate);

        // Ежемесячная процентная ставка
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        log.debug("Monthly rate calculated: {}", monthlyRate);

        // Аннуитетный коэффициент
        BigDecimal annuityCoefficient = monthlyRate.multiply((BigDecimal.ONE.add(monthlyRate)).pow(months))
                .divide((BigDecimal.ONE.add(monthlyRate)).pow(months).subtract(BigDecimal.ONE), 10, RoundingMode.HALF_UP);
        log.debug("Annuity coefficient calculated: {}", annuityCoefficient);

        // Ежемесячный аннуитетный платеж
        BigDecimal totalPayment = principal.multiply(annuityCoefficient).setScale(2, RoundingMode.HALF_UP);
        log.debug("Total monthly payment calculated: {}", totalPayment);

        // Процентная часть платежа
        BigDecimal interestPayment = remainingDebt.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        log.debug("Interest payment calculated: {}", interestPayment);

        // Основной долг
        BigDecimal debtPayment = totalPayment.subtract(interestPayment).setScale(2, RoundingMode.HALF_UP);
        log.debug("Debt payment calculated: {}", debtPayment);

        BigDecimal[] result = new BigDecimal[]{
                totalPayment,
                interestPayment,
                debtPayment
        };

        log.info("Monthly payment annuity calculation completed. Result: Total Payment: {}, Interest Payment: {}, Debt Payment: {}",
                result[0], result[1], result[2]);

        return result;
    }


    public BigDecimal calculateInsurance(BigDecimal amount, Integer term){
        /*
        Формула расчета страховки: baseCost + ((amount / 1000) * term)
        */
        log.info("Calculating insurance. Amount: {}, Term: {}", amount, term);

        try {
            BigDecimal insurance = loanProperties.getBaseCostOfInsurance()
                    .add((amount.divide(new BigDecimal("1000"), RoundingMode.HALF_UP))
                            .multiply(BigDecimal.valueOf(term)));
            log.info("Insurance calculated: {}", insurance);

            return insurance;

        } catch (Exception e) {
            log.error("Error calculating insurance. Amount: {}, Term: {}", amount, term, e);
            throw e;
        }
    }
}
