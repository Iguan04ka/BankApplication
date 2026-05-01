package ru.iguana.integrationroles.api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.iguana.integrationroles.api.dto.PasswordResetEmailDto;
import ru.iguana.integrationroles.api.kafka.KafkaProducer;
import ru.iguana.integrationroles.data.entity.PasswordResetTokenEntity;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.repository.PasswordResetTokenRepository;
import ru.iguana.integrationroles.data.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaProducer kafkaProducer;

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.password-reset.ttl-minutes:30}")
    private int ttlMinutes;

    /**
     * Public-facing entry point. Always succeeds silently to prevent email enumeration:
     * the caller cannot tell whether an email is actually registered.
     */
    @Transactional
    public void requestPasswordReset(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            log.info("Password reset requested with blank email — ignoring silently");
            return;
        }
        String email = rawEmail.trim().toLowerCase();

        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {} — silently ignoring", email);
            return;
        }

        UserEntity user = userOpt.get();
        String userSub = user.getUserKey().getSub();

        // Invalidate any previous unused tokens for the user — only the latest link should work.
        tokenRepository.invalidateAllForUser(userSub);

        String token = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");

        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setToken(token);
        entity.setUserSub(userSub);
        entity.setExpiresAt(Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES));
        entity.setUsed(false);
        tokenRepository.save(entity);

        String resetUrl = buildResetUrl(token);

        PasswordResetEmailDto message = new PasswordResetEmailDto()
                .setAddress(email)
                .setTheme("PASSWORD_RESET")
                .setResetUrl(resetUrl)
                .setToken(token)
                .setTtlMinutes(ttlMinutes)
                .setUserSub(userSub);

        kafkaProducer.sendPasswordResetEmail(message);
        log.info("Password reset email enqueued for sub={} (token expires at {})", userSub, entity.getExpiresAt());
    }

    @Transactional
    public boolean verifyToken(String token) {
        if (token == null || token.isBlank()) return false;
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isUsed())
                .filter(t -> Instant.now().isBefore(t.getExpiresAt()))
                .isPresent();
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        PasswordResetTokenEntity entity = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (entity.isUsed()) {
            throw new IllegalArgumentException("Token has already been used");
        }
        if (Instant.now().isAfter(entity.getExpiresAt())) {
            throw new IllegalArgumentException("Token has expired");
        }

        UserEntity user = userRepository.findByUserKey_Sub(entity.getUserSub())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        entity.setUsed(true);
        tokenRepository.save(entity);
        // Also invalidate any other tokens just in case
        tokenRepository.invalidateAllForUser(entity.getUserSub());

        log.info("Password reset completed for sub={}", entity.getUserSub());
    }

    private String buildResetUrl(String token) {
        String base = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        return base + "/auth/reset-password/" + token;
    }
}
