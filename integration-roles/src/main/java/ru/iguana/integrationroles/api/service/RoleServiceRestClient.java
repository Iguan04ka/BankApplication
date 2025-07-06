package ru.iguana.integrationroles.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.iguana.integrationroles.api.dto.UserWithRolesDto;
import ru.iguana.integrationroles.api.mapper.UserWithRolesMapper;
import ru.iguana.integrationroles.data.entity.RoleEntity;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.repository.RoleRepository;
import ru.iguana.integrationroles.data.repository.UserRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleServiceRestClient {

    private final RestTemplate restTemplate;

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final UserWithRolesMapper mapper;

    public void fetchAndLoadData() {
        try {
            ResponseEntity<List<UserWithRolesDto>> response =
                    restTemplate.exchange(
                            "http://role-model-service/api/users",
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            List<UserWithRolesDto> users = response.getBody();

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

            log.info("Загружено {} пользователей по REST", users.size());

        } catch (Exception e) {
            log.error("Ошибка при загрузке данных из Ролевой модели по REST: {}", e.getMessage());
        }
    }
}

