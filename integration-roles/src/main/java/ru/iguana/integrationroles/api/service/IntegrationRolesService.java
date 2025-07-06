package ru.iguana.integrationroles.api.service;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import ru.iguana.integrationroles.api.dto.UserResponseDto;
import ru.iguana.integrationroles.api.dto.UserWithRolesDto;
import ru.iguana.integrationroles.api.mapper.UserResponseMapper;
import ru.iguana.integrationroles.api.mapper.UserWithRolesMapper;
import ru.iguana.integrationroles.data.entity.RoleEntity;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.repository.RoleRepository;
import ru.iguana.integrationroles.data.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntegrationRolesService {
    private final UserRepository userRepository;
    private final UserResponseMapper rolesDtoMapper;

    @Cacheable(value = "usersWithRoles", key = "#ids")
    public Map<String, UserResponseDto> getUsersWithRolesByIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        user -> String.valueOf(user.getId()),  // преобразуем Long в String
                        rolesDtoMapper::toDto
                ));
    }

    public List<String> getUserLoginsByRole(String roleName) {
        return userRepository.findUserLoginsByRoleName(roleName);
    }

}
