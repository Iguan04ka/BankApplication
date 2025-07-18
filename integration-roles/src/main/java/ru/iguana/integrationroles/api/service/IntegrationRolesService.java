package ru.iguana.integrationroles.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.iguana.integrationroles.api.dto.UserResponseDto;
import ru.iguana.integrationroles.api.mapper.UserResponseMapper;
import ru.iguana.integrationroles.data.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationRolesService {

    private final UserRepository userRepository;
    private final UserResponseMapper rolesDtoMapper;

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
}


