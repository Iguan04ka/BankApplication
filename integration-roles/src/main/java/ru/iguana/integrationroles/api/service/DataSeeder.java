package ru.iguana.integrationroles.api.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.iguana.integrationroles.data.entity.RoleEntity;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.entity.UserKey;
import ru.iguana.integrationroles.data.entity.UserRole;
import ru.iguana.integrationroles.data.repository.RoleRepository;
import ru.iguana.integrationroles.data.repository.UserRepository;

import java.util.UUID;

/**
 * Сидер данных: создаёт базовые роли и учётную запись администратора
 * при первом запуске на пустой базе данных.
 * Все операции идемпотентны — существующие записи не затрагиваются.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final String ROLE_BASE_USER = "base_user";
    private static final String ROLE_ADMIN     = "admin";

    private static final String ADMIN_SUB      = "admin";
    private static final String ADMIN_EMAIL    = "iguana_2004@mail.ru";
    private static final String ADMIN_PASSWORD = "qweasdzxc";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        seedAdminUser();
    }

    // ── Роли ─────────────────────────────────────────────────────────────────

    private void seedRoles() {
        ensureRole(1L, ROLE_BASE_USER);
        ensureRole(2L, ROLE_ADMIN);
    }

    private void ensureRole(Long id, String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            RoleEntity role = new RoleEntity();
            role.setId(id);
            role.setName(name);
            roleRepository.save(role);
            log.info("DataSeeder: создана роль '{}'", name);
        } else {
            log.debug("DataSeeder: роль '{}' уже существует", name);
        }
    }

    // ── Администратор ─────────────────────────────────────────────────────────

    private void seedAdminUser() {
        if (userRepository.findByUserKey_Sub(ADMIN_SUB).isPresent()) {
            log.debug("DataSeeder: пользователь '{}' уже существует, пропуск", ADMIN_SUB);
            return;
        }

        RoleEntity adminRole = roleRepository.findByName(ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "DataSeeder: роль '" + ROLE_ADMIN + "' не найдена — проверьте seedRoles()"));

        UserEntity admin = new UserEntity();
        UserKey key = new UserKey();
        key.setSub(ADMIN_SUB);
        key.setSystemCode("DEFAULT");

        admin.setUserKey(key);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setBlocked(false);
        // Администратор всегда проходит 2FA при входе.
        admin.setTwoFactorEnabled(true);

        UserRole adminUserRole = new UserRole();
        adminUserRole.setUser(admin);
        adminUserRole.setRole(adminRole);
        admin.getRoles().add(adminUserRole);

        userRepository.save(admin);
        log.info("DataSeeder: создан администратор sub='{}', email='{}'", ADMIN_SUB, ADMIN_EMAIL);

        // Создаём минимальную запись в таблице client, чтобы администратор мог
        // в случае необходимости использовать и пользовательские эндпоинты.
        UUID clientId = UUID.randomUUID();
        entityManager.createNativeQuery("""
            INSERT INTO client (client_id, user_sub, email)
            VALUES (:clientId, :userSub, :email)
        """)
                .setParameter("clientId", clientId)
                .setParameter("userSub", ADMIN_SUB)
                .setParameter("email", ADMIN_EMAIL)
                .executeUpdate();

        log.info("DataSeeder: создана запись клиента для администратора (clientId={})", clientId);
    }
}
