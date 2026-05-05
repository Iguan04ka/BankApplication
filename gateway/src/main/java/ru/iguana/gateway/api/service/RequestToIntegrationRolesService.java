package ru.iguana.gateway.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.iguana.gateway.api.dto.ChangeEmailRequestDto;
import ru.iguana.gateway.api.dto.ChangePasswordRequestDto;
import ru.iguana.gateway.api.dto.ChangeSubRequestDto;
import ru.iguana.gateway.api.dto.ForgotPasswordRequestDto;
import ru.iguana.gateway.api.dto.LoginRequestDto;
import ru.iguana.gateway.api.dto.RegisterRequestDto;
import ru.iguana.gateway.api.dto.ResetPasswordRequestDto;
import ru.iguana.gateway.api.dto.RoleDto;
import ru.iguana.gateway.api.dto.TwoFactorVerifyRequestDto;
import ru.iguana.gateway.api.dto.UserResponseDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public void requestPasswordReset(ForgotPasswordRequestDto request) {
        rolesRestClient
                .post()
                .uri("/roles/forgotPassword")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public boolean verifyResetToken(String token) {
        Map<String, Boolean> resp = rolesRestClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/roles/verifyResetToken").queryParam("token", token).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return resp != null && Boolean.TRUE.equals(resp.get("valid"));
    }

    public void resetPassword(ResetPasswordRequestDto request) {
        rolesRestClient
                .post()
                .uri("/roles/resetPassword")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public Map<String, String> changeAccountSub(ChangeSubRequestDto request) {
        return rolesRestClient
                .post()
                .uri("/roles/account/changeSub")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, String> changeAccountEmail(ChangeEmailRequestDto request) {
        return rolesRestClient
                .post()
                .uri("/roles/account/changeEmail")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public void changeAccountPassword(ChangePasswordRequestDto request) {
        rolesRestClient
                .post()
                .uri("/roles/account/changePassword")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public Map<String, Object> getTwoFactorStatus(String sub) {
        return rolesRestClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/roles/2fa/status").queryParam("sub", sub).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public void issueTwoFactorLoginCode(String sub) {
        rolesRestClient
                .post()
                .uri("/roles/2fa/issueLoginCode")
                .body(Map.of("sub", sub))
                .retrieve()
                .toBodilessEntity();
    }

    public UserResponseDto verifyTwoFactorLoginCode(TwoFactorVerifyRequestDto request) {
        return rolesRestClient
                .post()
                .uri("/roles/2fa/verifyLoginCode")
                .body(request)
                .retrieve()
                .body(UserResponseDto.class);
    }

    public void issueTwoFactorEnableCode(String sub) {
        rolesRestClient
                .post()
                .uri("/roles/2fa/issueEnableCode")
                .body(Map.of("sub", sub))
                .retrieve()
                .toBodilessEntity();
    }

    public void confirmTwoFactorEnable(TwoFactorVerifyRequestDto request) {
        rolesRestClient
                .post()
                .uri("/roles/2fa/confirmEnable")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void issueTwoFactorDisableCode(String sub) {
        rolesRestClient
                .post()
                .uri("/roles/2fa/issueDisableCode")
                .body(Map.of("sub", sub))
                .retrieve()
                .toBodilessEntity();
    }

    public void confirmTwoFactorDisable(TwoFactorVerifyRequestDto request) {
        rolesRestClient
                .post()
                .uri("/roles/2fa/confirmDisable")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    // ── Admin proxies ────────────────────────────────────────────────────

    public List<UserResponseDto> adminListUsers() {
        return rolesRestClient
                .get()
                .uri("/roles/admin/users")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @CacheEvict(value = "users", key = "#sub")
    public UserResponseDto adminSetBlocked(String sub, boolean blocked) {
        return rolesRestClient
                .put()
                .uri("/roles/admin/users/{sub}/blocked", sub)
                .body(Map.of("blocked", blocked))
                .retrieve()
                .body(UserResponseDto.class);
    }

    @CacheEvict(value = "users", key = "#sub")
    public UserResponseDto adminSetRoles(String sub, Set<String> roleNames) {
        return rolesRestClient
                .put()
                .uri("/roles/admin/users/{sub}/roles", sub)
                .body(Map.of("roleNames", roleNames))
                .retrieve()
                .body(UserResponseDto.class);
    }

    public List<RoleDto> adminListRoles() {
        return rolesRestClient
                .get()
                .uri("/roles/admin/roles")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
