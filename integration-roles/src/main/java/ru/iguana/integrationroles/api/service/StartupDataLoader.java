package ru.iguana.integrationroles.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDataLoader implements ApplicationRunner {
    private final AdminClient adminClient;
    private final RoleServiceRestClient restClient;
    @Override
    public void run(ApplicationArguments args) {
        if (isKafkaAvailable()) {
            log.info("Kafka is available. Let's start via Kafka");
        } else {
            log.warn("\n" +
                    "Kafka is not available at startup. Switching to REST fallback");
            restClient.fetchAndLoadData();
        }
    }

    private boolean isKafkaAvailable() {
        try {
            DescribeTopicsResult result = adminClient.describeTopics(List.of("roles"));
            result.all().get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("Kafka is not available: {}", e.getMessage());
            return false;
        }
    }
}



