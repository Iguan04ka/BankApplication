package ru.iguana.deal.api.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.iguana.deal.api.dto.EmailMessageDto;
import ru.iguana.deal.api.service.DocumentService;
import ru.iguana.deal.kafka.KafkaProducer;
import ru.iguana.deal.model.entity.Client;
import ru.iguana.deal.model.entity.Statement;
import ru.iguana.deal.model.entity.enums.EmailTheme;
import ru.iguana.deal.model.repository.ClientRepository;
import ru.iguana.deal.model.repository.StatementRepository;

import java.util.UUID;

@RestController
@RequestMapping("/deal/document/{statementId}")
@AllArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/send")
    public void send(@PathVariable UUID statementId) {
        log.info("Received request to send documents for statementId: {}", statementId);
        documentService.sendDocuments(statementId);
    }

    @PostMapping("/sign")
    public void sign(@PathVariable UUID statementId) {
        log.info("Received request to sign documents for statementId: {}", statementId);
        documentService.signDocuments(statementId);
    }

    @PostMapping("/code")
    public void code(@PathVariable UUID statementId) {
        log.info("Received request to send SES code for statementId: {}", statementId);
        documentService.codeDocuments(statementId);
    }
}
