package ru.iguana.integrationroles.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private UserKeyDto userKey;
    private boolean blocked;
    private boolean twoFactorEnabled;
    private Set<RoleDto> roles;
}
