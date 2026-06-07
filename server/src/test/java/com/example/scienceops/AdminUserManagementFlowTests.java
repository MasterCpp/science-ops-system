package com.example.scienceops;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminUserManagementFlowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void superAdminCanCreateEditResetPasswordAssignRolesAndDisableAdminUser() {
        String superToken = login("superadmin", "password123");
        String username = "ops" + System.nanoTime();

        ResponseEntity<Map> createdResponse = exchange(HttpMethod.POST, "/api/admin/users", superToken, createUserPayload(username), Map.class);
        assertThat(createdResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> created = data(createdResponse);
        String userId = (String) created.get("id");
        assertThat(created.get("username")).isEqualTo(username);
        assertThat(created.get("displayName")).isEqualTo("New Ops User");
        assertThat(created.get("status")).isEqualTo("ENABLED");

        ResponseEntity<Map> duplicate = exchange(HttpMethod.POST, "/api/admin/users", superToken, createUserPayload(username), Map.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("CONFLICT");

        ResponseEntity<Map> updatedResponse = exchange(
                HttpMethod.PUT,
                "/api/admin/users/" + userId,
                superToken,
                Map.of("displayName", "Updated Ops User", "phone", "13900008888", "status", "ENABLED"),
                Map.class
        );
        assertThat(updatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(updatedResponse).get("displayName")).isEqualTo("Updated Ops User");

        ResponseEntity<Map> rolesResponse = exchange(
                HttpMethod.PUT,
                "/api/admin/users/" + userId + "/roles",
                superToken,
                Map.of("roleCodes", List.of("ACTIVITY_ADMIN")),
                Map.class
        );
        assertThat(rolesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rolesResponse.getBody().toString()).contains("ACTIVITY_ADMIN");

        ResponseEntity<Map> resetResponse = exchange(
                HttpMethod.POST,
                "/api/admin/users/" + userId + "/reset-password",
                superToken,
                Map.of("password", "newpass123"),
                Map.class
        );
        assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login(username, "newpass123")).isNotBlank();

        ResponseEntity<Map> filtered = exchange(
                HttpMethod.GET,
                "/api/admin/users?keyword=Updated&status=ENABLED&roleCode=ACTIVITY_ADMIN&page=1&pageSize=10",
                superToken,
                null,
                Map.class
        );
        assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(items(filtered)).anySatisfy(item -> assertThat(item.get("username")).isEqualTo(username));

        ResponseEntity<Map> disabledResponse = exchange(
                HttpMethod.PUT,
                "/api/admin/users/" + userId,
                superToken,
                Map.of("displayName", "Updated Ops User", "phone", "13900008888", "status", "DISABLED"),
                Map.class
        );
        assertThat(disabledResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(disabledResponse).get("status")).isEqualTo("DISABLED");

        ResponseEntity<Map> disabledLogin = loginResponse(username, "newpass123");
        assertThat(disabledLogin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(disabledLogin.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    void superAdminCanViewRolesAndPermissions() {
        String superToken = login("superadmin", "password123");

        ResponseEntity<Map> roles = exchange(HttpMethod.GET, "/api/admin/roles", superToken, null, Map.class);
        assertThat(roles.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(roles.getBody().toString()).contains("SUPER_ADMIN", "ACTIVITY_ADMIN", "VOLUNTEER_ADMIN", "admin-user:manage");

        ResponseEntity<Map> permissions = exchange(HttpMethod.GET, "/api/admin/permissions", superToken, null, Map.class);
        assertThat(permissions.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(permissions.getBody().toString()).contains("admin-user:manage", "operation-log:view", "activity:manage");
    }

    @Test
    void activityAndVolunteerAdminsCannotManageAdminUsers() {
        String activityToken = login("activityadmin", "password123");
        String volunteerToken = login("volunteeradmin", "password123");

        assertThat(exchange(HttpMethod.GET, "/api/admin/users", activityToken, null, Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange(HttpMethod.GET, "/api/admin/users", volunteerToken, null, Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange(HttpMethod.GET, "/api/admin/roles", activityToken, null, Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange(HttpMethod.GET, "/api/admin/permissions", volunteerToken, null, Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Map<String, Object> createUserPayload(String username) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("displayName", "New Ops User");
        payload.put("phone", "13900007777");
        payload.put("status", "ENABLED");
        payload.put("password", "password123");
        return payload;
    }

    private String login(String username, String password) {
        ResponseEntity<Map> response = loginResponse(username, password);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) data(response).get("token");
    }

    private ResponseEntity<Map> loginResponse(String username, String password) {
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/api/admin/auth/login",
                json(Map.of("username", username, "password", password)),
                Map.class
        );
    }

    private <T> ResponseEntity<T> exchange(HttpMethod method, String path, String token, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                method,
                new HttpEntity<>(body, headers),
                responseType
        );
    }

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private Map<?, ?> data(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (Map<?, ?>) response.getBody().get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<?, ?>> items(ResponseEntity<Map> response) {
        return (List<Map<?, ?>>) data(response).get("items");
    }
}
