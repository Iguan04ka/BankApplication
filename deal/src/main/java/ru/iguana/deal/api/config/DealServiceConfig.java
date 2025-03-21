package ru.iguana.deal.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import ru.iguana.deal.api.convertor.ClientConvertor;
import ru.iguana.deal.api.convertor.CreditConvertor;
import ru.iguana.deal.api.convertor.ScoringDataDtoConvertor;
import ru.iguana.deal.api.convertor.StatementConvertor;
import ru.iguana.deal.api.service.CalculateCreditService;
import ru.iguana.deal.api.service.StatementService;
import ru.iguana.deal.kafka.KafkaProducer;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.CreditRepository;
import ru.iguana.deal.model.repository.StatementRepository;

@Configuration
public class DealServiceConfig {

    @Bean
    public WebClient webClient(DealProperties dealProperties){
        return WebClient.builder().baseUrl(dealProperties.getBaseUrl()).build();
    }

    @Bean
    public CalculateCreditService calculateCreditService(StatementRepository statementRepository,
                                                         ClientRepository clientRepository,
                                                         CreditConvertor creditConvertor,
                                                         CreditRepository creditRepository,
                                                         ScoringDataDtoConvertor scoringDataDtoConvertor,
                                                         KafkaProducer kafkaProducer,
                                                         WebClient webClient) {
        return new CalculateCreditService(
                statementRepository,
                clientRepository,
                creditConvertor,
                creditRepository,
                scoringDataDtoConvertor,
                kafkaProducer,
                webClient
        );
    }

    @Bean
    public StatementService statementService(WebClient webClient,
                                             StatementConvertor statementConvertor,
                                             ClientConvertor clientConvertor,
                                             ClientRepository clientRepository,
                                             StatementRepository statementRepository,
                                             DealProperties dealProperties) {
        return new StatementService(
                webClient,
                statementConvertor,
                clientConvertor,
                clientRepository,
                statementRepository,
                dealProperties
        );
    }
}
