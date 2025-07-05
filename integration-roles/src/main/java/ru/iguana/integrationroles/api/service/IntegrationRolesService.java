package ru.iguana.integrationroles.api.service;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
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

    private final UserWithRolesMapper mapper;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserResponseMapper rolesDtoMapper;

    public void saveUsers(List<UserWithRolesDto> users){

        for (UserWithRolesDto dto : users) {
            for (UserWithRolesDto.RoleWrapper wrapper : dto.getRoles()) {
                RoleEntity role = wrapper.getRole();
                if (!roleRepository.existsById(role.getId())) {
                    roleRepository.save(role);
                }
            }
        }

        List<UserEntity> usersEntity = mapper.mapToUserEntities(users);

        userRepository.saveAll(usersEntity);

    }

    public Map<Long, UserResponseDto> getUsersWithRolesByIds(List<Long> ids) {
        List<UserEntity> users = userRepository.findAllById(ids);

        return users.stream()
                .collect(Collectors.toMap(
                        UserEntity::getId,
                        rolesDtoMapper::toDto
                ));
    }

    public List<String> getUserLoginsByRole(String roleName) {
        return userRepository.findUserLoginsByRoleName(roleName);
    }

}
