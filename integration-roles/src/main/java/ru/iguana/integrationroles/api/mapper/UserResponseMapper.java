package ru.iguana.integrationroles.api.mapper;

import org.mapstruct.*;
import ru.iguana.integrationroles.api.dto.UserResponseDto;
import ru.iguana.integrationroles.api.dto.UserKeyDto;
import ru.iguana.integrationroles.api.dto.RoleDto;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.entity.UserRole;
import ru.iguana.integrationroles.data.entity.RoleEntity;
import ru.iguana.integrationroles.data.entity.UserKey;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserResponseMapper {

    @Mapping(source = "userKey", target = "userKey")
    @Mapping(source = "blocked", target = "blocked")
    @Mapping(target = "roles", expression = "java(mapRoles(entity.getRoles()))")
    UserResponseDto toDto(UserEntity entity);

    default Set<RoleDto> mapRoles(Set<UserRole> userRoles) {
        return userRoles.stream()
                .map(UserRole::getRole)
                .map(this::mapRoleToDto)
                .collect(Collectors.toSet());
    }

    default RoleDto mapRoleToDto(RoleEntity role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        return dto;
    }

    default UserKeyDto map(UserKey userKey) {
        if (userKey == null) return null;
        UserKeyDto dto = new UserKeyDto();
        dto.setSub(userKey.getSub());
        dto.setSystemCode(userKey.getSystemCode());
        return dto;
    }
}
