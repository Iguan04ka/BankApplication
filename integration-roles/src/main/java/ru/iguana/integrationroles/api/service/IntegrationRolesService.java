package ru.iguana.integrationroles.api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.iguana.integrationroles.api.dto.LoginRequestDto;
import ru.iguana.integrationroles.api.dto.RegisterRequestDto;
import ru.iguana.integrationroles.api.dto.UserResponseDto;
import ru.iguana.integrationroles.api.mapper.UserResponseMapper;
import ru.iguana.integrationroles.data.entity.RoleEntity;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.entity.UserKey;
import ru.iguana.integrationroles.data.entity.UserRole;
import ru.iguana.integrationroles.data.repository.RoleRepository;
import ru.iguana.integrationroles.data.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationRolesService {

    private final UserRepository userRepository;
    private final UserResponseMapper rolesDtoMapper;

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Cacheable(value = "usersWithRoles", key = "#ids")
    public Map<String, UserResponseDto> getUsersWithRolesByIds(List<Long> ids) {
        log.info("getUsersWithRolesByIds called with ids: {}", ids);
        var users = userRepository.findAllById(ids);
        var result = users.stream()
                .collect(Collectors.toMap(
                        user -> String.valueOf(user.getId()),
                        rolesDtoMapper::toDto
                ));
        log.info("getUsersWithRolesByIds result: {}", result);
        return result;
    }

    public List<String> getUserLoginsByRole(String roleName) {
        log.info("getUserLoginsByRole called with roleName: {}", roleName);
        var logins = userRepository.findUserLoginsByRoleName(roleName);
        log.info("getUserLoginsByRole result: {}", logins);
        return logins;
    }
    public UserResponseDto getUserResponseDtoBySub(String sub){
        Optional<UserEntity> userEntityOptional = userRepository.findByUserKey_Sub(sub);
        UserEntity userEntity = userEntityOptional.orElseThrow(() -> new IllegalArgumentException("Значение не найдено"));
        return rolesDtoMapper.toDto(userEntity);
    }

    @Transactional
    public UserResponseDto createUser(RegisterRequestDto request) {

        if (request.getSub() == null || request.getSub().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Sub and password are required");
        }

        if (userRepository.findByUserKey_Sub(request.getSub()).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }

        UserEntity user = new UserEntity();

        UserKey key = new UserKey();
        key.setSub(request.getSub());
        key.setSystemCode("DEFAULT");

        user.setUserKey(key);
        user.setBlocked(false);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        RoleEntity baseRole = roleRepository.findByName("base_user")
                .orElseThrow(() -> new IllegalStateException("Base role not found"));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(baseRole);

        user.getRoles().add(userRole);

        userRepository.save(user);
        UUID clientId = UUID.randomUUID();
        String userSub = user.getUserKey().getSub();

        entityManager.createNativeQuery("""
        INSERT INTO client (client_id, user_sub)
        VALUES (:clientId, :userSub)
    """)
                .setParameter("clientId", clientId)
                .setParameter("userSub", userSub)
                .executeUpdate();

        return rolesDtoMapper.toDto(user);
    }

    public UserResponseDto authenticate(LoginRequestDto request) {

        if (request.getSub() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        UserEntity user = userRepository.findByUserKey_Sub(request.getSub())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return rolesDtoMapper.toDto(user);
    }
}


