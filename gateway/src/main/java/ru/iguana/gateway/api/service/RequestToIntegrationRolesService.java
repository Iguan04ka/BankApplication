package ru.iguana.gateway.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.iguana.gateway.api.dto.LoginRequestDto;
import ru.iguana.gateway.api.dto.RegisterRequestDto;
import ru.iguana.gateway.api.dto.UserResponseDto;

import java.util.List;
import java.util.Map;

@Service
public class RequestToIntegrationRolesService {

    private final RestClient rolesRestClient;

    public RequestToIntegrationRolesService(@Qualifier("rolesRestClient") RestClient restClient) {
        this.rolesRestClient = restClient;
    }

    public Map<String, UserResponseDto> getUsersRolesByIds(List<Long> ids) {
        return rolesRestClient.post()
                .uri("/roles/usersRoles")
                .body(ids)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
    @Cacheable(value = "users", key = "#sub")
    public UserResponseDto getUserBySub(String sub){
        return rolesRestClient
                .post()
                .uri("/roles/userBySub")
                .body(Map.of("sub", sub))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public UserResponseDto authenticate(LoginRequestDto request) {
        return rolesRestClient
                .post()
                .uri("/roles/authenticate")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
    public UserResponseDto createUser(RegisterRequestDto request) {
        return rolesRestClient
                .post()
                .uri("/roles/createUser")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

}
