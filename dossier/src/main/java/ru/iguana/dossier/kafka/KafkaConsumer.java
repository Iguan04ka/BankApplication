package ru.iguana.dossier.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.iguana.dossier.service.EmailTemplateService;
import ru.iguana.dossier.service.MailSenderService;

import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final MailSenderService mailSenderService;
    private final MessageConvertor messageConvertor;
    private final EmailTemplateService templateService;

    @KafkaListener(topics = "finish-registration", groupId = "dossier")
    public void listenFinishRegistrationTopic(String message) {
        log.info("Received message from topic 'finish-registration'");
        log.debug("Message: {}", message);
        sendFinishRegistration(message);
    }

    @KafkaListener(topics = "send-documents", groupId = "dossier")
    public void listenSendDocumentsTopic(String message) {
        log.info("Received message from topic 'send-documents'");
        log.debug("Message: {}", message);
        sendPlain(message, "Оформление документов");
    }

    @KafkaListener(topics = "create-documents", groupId = "dossier")
    public void listenCreateDocumentsTopic(String message) {
        log.info("Received message from topic 'create-documents'");
        log.debug("Message: {}", message);
        sendPlain(message, "Документы по кредиту");
    }

    @KafkaListener(topics = "send-ses", groupId = "dossier")
    public void listenSendSesTopic(String message) {
        log.info("Received message from topic 'send-ses'");
        log.debug("Message: {}", message);
        sendSesConfirmation(message);
    }

    private void sendFinishRegistration(String message) {
        try {
            String address = messageConvertor.getAddress(message);

            Map<String, Object> vars = new HashMap<>();
            vars.put("greetingName", messageConvertor.getGreetingName(message));
            vars.put("amount", messageConvertor.getFormattedAmount(message));
            vars.put("term", messageConvertor.getTerm(message));
            vars.put("rate", messageConvertor.getFormattedRate(message));
            vars.put("monthlyPayment", messageConvertor.getFormattedMonthlyPayment(message));

            String html = templateService.render("finish-registration", vars);
            mailSenderService.sendHtmlEmail(address, "Ваше кредитное предложение — Атлас Банк", html);
        } catch (Exception e) {
            log.error("Failed to send FINISH_REGISTRATION email: {}. Error: {}", message, e.getMessage(), e);
        }
    }

    private void sendSesConfirmation(String message) {
        try {
            String address = messageConvertor.getAddress(message);

            Map<String, Object> vars = new HashMap<>();
            vars.put("greetingName", messageConvertor.getGreetingName(message));
            vars.put("code", messageConvertor.getCode(message));
            Integer ttl = messageConvertor.getTtlMinutes(message);
            vars.put("ttlMinutes", ttl != null ? ttl : 5);
            vars.put("amount", messageConvertor.getFormattedAmount(message));
            vars.put("term", messageConvertor.getTerm(message));
            vars.put("monthlyPayment", messageConvertor.getFormattedMonthlyPayment(message));

            String html = templateService.render("ses-confirmation", vars);
            mailSenderService.sendHtmlEmail(address, "Код подтверждения — Атлас Банк", html);
        } catch (Exception e) {
            log.error("Failed to send SES email: {}. Error: {}", message, e.getMessage(), e);
        }
    }

    private void sendPlain(String message, String subject) {
        try {
            String address = messageConvertor.getAddress(message);
            String text = messageConvertor.getText(message);
            mailSenderService.sendEmail(address, subject, text);
        } catch (Exception e) {
            log.error("Failed to process plain message: {}. Error: {}", message, e.getMessage(), e);
        }
    }
}
