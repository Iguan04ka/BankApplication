package ru.iguana.deal.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.iguana.deal.api.service.DocumentService;


import java.util.UUID;

@RestController
@RequestMapping("/deal/document/{statementId}")
@AllArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/send")
    @Operation(summary = "Send documents", description = "Sends documents to the user for signing based on the provided statementId.")
    public void send(@PathVariable UUID statementId) {
        log.info("Received request to send documents for statementId: {}", statementId);
        documentService.sendDocuments(statementId);
    }

    @PostMapping("/sign")
    @Operation(summary = "Sign documents", description = "Signs the user's documents based on the provided statementId.")
    public void sign(@PathVariable UUID statementId) {
        log.info("Received request to sign documents for statementId: {}", statementId);
        documentService.signDocuments(statementId);
    }

    @PostMapping("/code")
    @Operation(summary = "Send SES code", description = "Sends an SES code to the user for document signing confirmation.")
    public void code(@PathVariable UUID statementId) {
        log.info("Received request to send SES code for statementId: {}", statementId);
        documentService.codeDocuments(statementId);
    }
}
