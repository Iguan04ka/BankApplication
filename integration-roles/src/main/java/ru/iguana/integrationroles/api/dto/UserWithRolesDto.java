package ru.iguana.integrationroles.api.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import ru.iguana.integrationroles.data.entity.RoleEntity;
import ru.iguana.integrationroles.data.entity.UserEntity;

import java.util.List;

@Data
public class UserWithRolesDto {
    private UserEntity user;
    private List<RoleWrapper> roles;

    @Setter
    @Getter
    public static class RoleWrapper {
        private RoleEntity role;
    }
}
