package ru.iguana.integrationroles.api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.iguana.integrationroles.api.dto.RoleDto;
import ru.iguana.integrationroles.api.dto.UserResponseDto;
import ru.iguana.integrationroles.api.mapper.UserResponseMapper;
import ru.iguana.integrationroles.data.entity.RoleEntity;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.entity.UserRole;
import ru.iguana.integrationroles.data.repository.RoleRepository;
import ru.iguana.integrationroles.data.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for admin-only operations: listing users, blocking/unblocking,
 * managing roles, listing roles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRolesService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserResponseMapper mapper;

    public List<UserResponseDto> listAllUsers() {
        log.info("Admin: listAllUsers");
        return userRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public UserResponseDto setBlocked(String sub, boolean blocked) {
        log.info("Admin: setBlocked sub={} blocked={}", sub, blocked);
        UserEntity user = userRepository.findByUserKey_Sub(sub)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setBlocked(blocked);
        userRepository.save(user);
        return mapper.toDto(user);
    }

    @Transactional
    public UserResponseDto setRoles(String sub, Set<String> roleNames) {
        log.info("Admin: setRoles sub={} roles={}", sub, roleNames);
        UserEntity user = userRepository.findByUserKey_Sub(sub)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }

        Set<RoleEntity> targetRoles = new HashSet<>();
        for (String name : roleNames) {
            RoleEntity role = roleRepository.findByName(name)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + name));
            targetRoles.add(role);
        }

        // remove user-roles whose role is not in target
        user.getRoles().removeIf(ur -> !targetRoles.contains(ur.getRole()));

        // existing role names after removal
        Set<String> existing = new HashSet<>();
        for (UserRole ur : user.getRoles()) {
            existing.add(ur.getRole().getName());
        }

        // add new ones
        for (RoleEntity r : targetRoles) {
            if (!existing.contains(r.getName())) {
                UserRole ur = new UserRole();
                ur.setUser(user);
                ur.setRole(r);
                user.getRoles().add(ur);
            }
        }

        // If user is becoming admin, force-enable 2FA so admin login always uses 2FA path
        if (roleNames.contains("admin")) {
            user.setTwoFactorEnabled(true);
        }

        userRepository.save(user);
        return mapper.toDto(user);
    }

    public List<RoleDto> listAllRoles() {
        return roleRepository.findAll().stream()
                .map(r -> {
                    RoleDto dto = new RoleDto();
                    dto.setId(r.getId());
                    dto.setName(r.getName());
                    return dto;
                })
                .toList();
    }
}
