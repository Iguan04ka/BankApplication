package ru.iguana.integrationroles.api.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.repository.UserRepository;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public String changeSub(String currentSub, String newSub, String currentPassword) {
        if (currentSub == null || currentSub.isBlank()) {
            throw new IllegalArgumentException("Current sub is required");
        }
        if (newSub == null || newSub.isBlank()) {
            throw new IllegalArgumentException("New login is required");
        }
        UserEntity user = authenticate(currentSub, currentPassword);

        String trimmed = newSub.trim();
        if (trimmed.equals(currentSub)) {
            return currentSub;
        }
        if (userRepository.findByUserKey_Sub(trimmed).isPresent()) {
            throw new IllegalArgumentException("Login is already in use");
        }

        user.getUserKey().setSub(trimmed);
        userRepository.save(user);

        // Keep deal-side client.user_sub in sync
        entityManager.createNativeQuery("UPDATE client SET user_sub = :newSub WHERE user_sub = :oldSub")
                .setParameter("newSub", trimmed)
                .setParameter("oldSub", currentSub)
                .executeUpdate();

        log.info("User {} changed login to {}", currentSub, trimmed);
        return trimmed;
    }

    @Transactional
    public String changeEmail(String currentSub, String newEmail, String currentPassword) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("New email is required");
        }
        UserEntity user = authenticate(currentSub, currentPassword);

        String email = newEmail.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (email.equalsIgnoreCase(user.getEmail())) {
            return user.getEmail();
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }

        user.setEmail(email);
        userRepository.save(user);

        // Keep deal-side client.email in sync (column exists on the client table)
        entityManager.createNativeQuery("UPDATE client SET email = :email WHERE user_sub = :sub")
                .setParameter("email", email)
                .setParameter("sub", currentSub)
                .executeUpdate();

        log.info("User {} changed email to {}", currentSub, email);
        return email;
    }

    @Transactional
    public void changePassword(String currentSub, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        UserEntity user = authenticate(currentSub, currentPassword);

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must differ from the current one");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("User {} changed password", currentSub);
    }

    private UserEntity authenticate(String sub, String currentPassword) {
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("Authenticated user not provided");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password is required");
        }
        UserEntity user = userRepository.findByUserKey_Sub(sub)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (user.isBlocked()) {
            throw new IllegalArgumentException("User is blocked");
        }
        return user;
    }
}
