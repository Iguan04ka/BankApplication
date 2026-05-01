package ru.iguana.dossier.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailSenderService {
    private final JavaMailSender javaMailSender;

    public void sendEmail(String to, String subject, String text) {
        sendInternal(to, subject, text, false);
    }

    public void sendHtmlEmail(String to, String subject, String html) {
        sendInternal(to, subject, html, true);
    }

    private void sendInternal(String to, String subject, String body, boolean html) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, html);

            javaMailSender.send(message);
            log.info("Email sent successfully to: {} (html={})", to, html);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }
}
