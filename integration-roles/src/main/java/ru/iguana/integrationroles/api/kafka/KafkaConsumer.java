package ru.iguana.integrationroles.api.kafka;

import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.iguana.integrationroles.api.dto.UserWithRolesDto;
import ru.iguana.integrationroles.api.mapper.UserWithRolesMapper;
import ru.iguana.integrationroles.data.entity.RoleEntity;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.repository.RoleRepository;
import ru.iguana.integrationroles.data.repository.UserRepository;

import java.util.List;
@Service
@AllArgsConstructor
public class KafkaConsumer {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserWithRolesMapper mapper;

    @KafkaListener(topics = "roles", groupId = "saveRoles")
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
}


