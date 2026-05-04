package ru.iguana.integrationroles.api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.iguana.integrationroles.api.dto.TwoFactorEmailDto;
import ru.iguana.integrationroles.api.kafka.KafkaProducer;
import ru.iguana.integrationroles.data.entity.TwoFactorChallengeEntity;
import ru.iguana.integrationroles.data.entity.TwoFactorPurpose;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.repository.TwoFactorChallengeRepository;
import ru.iguana.integrationroles.data.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TwoFactorChallengeRepository challengeRepository;
    private final KafkaProducer kafkaProducer;

    @Value("${app.two-factor.ttl-minutes:5}")
    private int ttlMinutes;

    public boolean isEnabled(String sub) {
        return userRepository.findByUserKey_Sub(sub)
                .map(UserEntity::isTwoFactorEnabled)
                .orElse(false);
    }

    public String getEmail(String sub) {
        return userRepository.findByUserKey_Sub(sub)
                .map(UserEntity::getEmail)
                .orElse(null);
    }

    /**
     * Issues a code for a given purpose. Always returns silently — even if the user does not exist —
     * for the LOGIN purpose, to avoid leaking account existence. For settings flows the caller has
     * already authenticated via JWT so the user is guaranteed to exist.
     */
    @Transactional
    public void issueCode(String sub, TwoFactorPurpose purpose) {
        Optional<UserEntity> userOpt = userRepository.findByUserKey_Sub(sub);
        if (userOpt.isEmpty()) {
            log.info("2FA: no user for sub={} (purpose={}) — silently ignoring", sub, purpose);
            return;
        }
        UserEntity user = userOpt.get();
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("2FA: user {} has no email — cannot send code (purpose={})", sub, purpose);
            throw new IllegalStateException("Account has no email configured");
        }

        // Invalidate previous unused challenges for this purpose
        challengeRepository.invalidateAllByUserSubAndPurpose(sub, purpose);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        TwoFactorChallengeEntity challenge = new TwoFactorChallengeEntity();
        challenge.setUserSub(sub);
        challenge.setPurpose(purpose);
        challenge.setCode(code);
        challenge.setExpiresAt(Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES));
        challenge.setUsed(false);
        challengeRepository.save(challenge);

        TwoFactorEmailDto message = new TwoFactorEmailDto()
                .setAddress(email)
                .setTheme("TWO_FACTOR_CODE")
                .setPurpose(purpose.name())
                .setCode(code)
                .setTtlMinutes(ttlMinutes)
                .setUserSub(sub);

        kafkaProducer.sendTwoFactorCode(message);
        log.info("2FA: code issued for sub={} purpose={} (expires {})",
                sub, purpose, challenge.getExpiresAt());
    }

    /**
     * Verifies a code without changing the user's 2FA state. Used for login.
     * Returns true if the code matches an active challenge of the LOGIN purpose;
     * marks the challenge as used.
     */
    @Transactional
    public boolean verifyCode(String sub, String code, TwoFactorPurpose purpose) {
        if (sub == null || sub.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        Optional<TwoFactorChallengeEntity> opt = challengeRepository
                .findFirstByUserSubAndPurposeAndUsedFalseOrderByCreatedAtDesc(sub, purpose);
        if (opt.isEmpty()) return false;

        TwoFactorChallengeEntity challenge = opt.get();
        if (Instant.now().isAfter(challenge.getExpiresAt())) {
            log.info("2FA: code expired for sub={} purpose={}", sub, purpose);
            return false;
        }
        if (!challenge.getCode().equals(code.trim())) {
            log.info("2FA: code mismatch for sub={} purpose={}", sub, purpose);
            return false;
        }

        challenge.setUsed(true);
        challengeRepository.save(challenge);
        log.info("2FA: code verified for sub={} purpose={}", sub, purpose);
        return true;
    }

    /**
     * Verifies the code AND sets twoFactorEnabled=true.
     */
    @Transactional
    public boolean confirmEnable(String sub, String code) {
        if (!verifyCode(sub, code, TwoFactorPurpose.ENABLE_2FA)) return false;
        UserEntity user = userRepository.findByUserKey_Sub(sub).orElseThrow();
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        log.info("2FA enabled for user sub={}", sub);
        return true;
    }

    /**
     * Verifies the code AND sets twoFactorEnabled=false.
     */
    @Transactional
    public boolean confirmDisable(String sub, String code) {
        if (!verifyCode(sub, code, TwoFactorPurpose.DISABLE_2FA)) return false;
        UserEntity user = userRepository.findByUserKey_Sub(sub).orElseThrow();
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        log.info("2FA disabled for user sub={}", sub);
        return true;
    }
}
