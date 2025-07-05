package ru.iguana.integrationroles.api.dto;

import lombok.Data;

import javax.management.relation.Role;
import java.util.Set;

@Data
public class UserResponseDto {
    private UserKeyDto userKey;
    private boolean blocked;
    private Set<RoleDto> roles;
}
