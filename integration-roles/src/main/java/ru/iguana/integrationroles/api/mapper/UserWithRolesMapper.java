package ru.iguana.integrationroles.api.mapper;

import org.mapstruct.*;
import ru.iguana.integrationroles.api.dto.UserWithRolesDto;
import ru.iguana.integrationroles.data.entity.UserEntity;
import ru.iguana.integrationroles.data.entity.UserRole;
import ru.iguana.integrationroles.data.entity.RoleEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserWithRolesMapper {

    default Set<UserRole> mapToUserRoles(UserWithRolesDto dto, UserEntity user) {
        return dto.getRoles()
                .stream()
                .map(wrapper -> {
                    RoleEntity role = wrapper.getRole();
                    if (role.getId() == null) {
                        throw new IllegalArgumentException("Role id must be set in DTO");
                    }
                    return buildUserRole(user, role);
                })
                .collect(Collectors.toSet());
    }

    default UserEntity mapFullUserEntity(UserWithRolesDto dto) {
        UserEntity user = dto.getUser();
        Set<UserRole> userRoles = mapToUserRoles(dto, user);
        user.setRoles(userRoles);
        return user;
    }

    default List<UserEntity> mapToUserEntities(List<UserWithRolesDto> dtoList) {
        return dtoList.stream()
                .map(this::mapFullUserEntity)
                .collect(Collectors.toList());
    }

    default UserRole buildUserRole(UserEntity user, RoleEntity role) {
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        return userRole;
    }
}