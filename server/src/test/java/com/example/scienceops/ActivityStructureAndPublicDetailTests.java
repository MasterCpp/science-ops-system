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
class ActivityStructureAndPublicDetailTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void adminCanManageProcessItemsAndCustomFields() {
        String token = login("activityadmin");
        String activityId = (String) createActivity(token, "Structured Activity", 100, "2026-12-01T18:00:00").get("id");

        Map<?, ?> second = post(token, "/api/admin/activities/" + activityId + "/process-items", processPayload("15:00", "Experiment", 20));
        Map<?, ?> first = post(token, "/api/admin/activities/" + activityId + "/process-items", processPayload("14:00", "Opening", 10));
        assertThat(second.get("title")).isEqualTo("Experiment");

        put(token, "/api/admin/activities/" + activityId + "/process-items/" + second.get("id"), processPayload("15:10", "Experiment Updated", 30));
        List<?> processItems = dataList(exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/process-items", token, null));
        assertThat(((Map<?, ?>) processItems.get(0)).get("id")).isEqualTo(first.get("id"));
        assertThat(((Map<?, ?>) processItems.get(1)).get("title")).isEqualTo("Experiment Updated");

        delete(token, "/api/admin/activities/" + activityId + "/process-items/" + first.get("id"));
        List<?> afterDelete = dataList(exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/process-items", token, null));
        assertThat(afterDelete).hasSize(1);

        Map<?, ?> grade = post(token, "/api/admin/activities/" + activityId + "/custom-fields", customFieldPayload("grade", "Grade", "SELECT", true, 10));
        assertThat(grade.get("fieldKey")).isEqualTo("grade");
        assertThat(grade.get("options").toString()).isEqualTo("[A, B]");

        ResponseEntity<Map> duplicate = exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/custom-fields", token, customFieldPayload("grade", "Duplicate Grade", "TEXT", false, 20));
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("CONFLICT");

        put(token, "/api/admin/activities/" + activityId + "/custom-fields/" + grade.get("id"), customFieldPayload("grade_level", "Grade Level", "TEXT", false, 5));
        List<?> fields = dataList(exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/custom-fields", token, null));
        assertThat(((Map<?, ?>) fields.get(0)).get("fieldKey")).isEqualTo("grade_level");
    }

    @Test
    void publicActivityDetailReturnsStructureAndRegistrationAvailability() {
        String token = login("activityadmin");
        String activityId = (String) createActivity(token, "Public Detail Activity", 30, "2026-12-01T18:00:00").get("id");
        post(token, "/api/admin/activities/" + activityId + "/process-items", processPayload("14:00", "Opening", 1));
        post(token, "/api/admin/activities/" + activityId + "/custom-fields", customFieldPayload("school", "School", "TEXT", true, 1));
        post(token, "/api/admin/activities/" + activityId + "/publish", null);

        ResponseEntity<Map> response = restTemplate.getForEntity("http://localhost:" + port + "/api/mobile/activities/" + activityId, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = data(response);
        assertThat(data.get("title")).isEqualTo("Public Detail Activity");
        assertThat(data.get("registrationAvailability")).isEqualTo("OPEN");
        assertThat(data.get("remainingCapacity")).isEqualTo(30);
        assertThat((List<?>) data.get("processItems")).hasSize(1);
        assertThat((List<?>) data.get("customFields")).hasSize(1);
    }

    @Test
    void publicActivityDetailShowsUnavailableStates() {
        String token = login("activityadmin");
        String draftId = (String) createActivity(token, "Draft Public Activity", 10, "2026-12-01T18:00:00").get("id");
        String expiredId = (String) createActivity(token, "Expired Public Activity", 10, "2020-01-01T18:00:00").get("id");
        post(token, "/api/admin/activities/" + expiredId + "/publish", null);

        Map<?, ?> draft = data(restTemplate.getForEntity("http://localhost:" + port + "/api/mobile/activities/" + draftId, Map.class));
        assertThat(draft.get("registrationAvailability")).isEqualTo("CLOSED");

        Map<?, ?> expired = data(restTemplate.getForEntity("http://localhost:" + port + "/api/mobile/activities/" + expiredId, Map.class));
        assertThat(expired.get("registrationAvailability")).isEqualTo("DEADLINE_PASSED");
    }

    private Map<?, ?> createActivity(String token, String title, Integer capacity, String deadline) {
        ResponseEntity<Map> response = exchange(HttpMethod.POST, "/api/admin/activities", token, activityPayload(title, capacity, deadline));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return data(response);
    }

    private Map<?, ?> post(String token, String path, Object payload) {
        ResponseEntity<Map> response = exchange(HttpMethod.POST, path, token, payload);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return data(response);
    }

    private Map<?, ?> put(String token, String path, Object payload) {
        ResponseEntity<Map> response = exchange(HttpMethod.PUT, path, token, payload);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return data(response);
    }

    private void delete(String token, String path) {
        ResponseEntity<Map> response = exchange(HttpMethod.DELETE, path, token, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Map<String, Object> activityPayload(String title, Integer capacity, String deadline) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", "Activity description");
        payload.put("startTime", "2026-11-01T09:00:00");
        payload.put("endTime", "2026-11-01T12:00:00");
        payload.put("location", "Science Hall");
        payload.put("capacity", capacity);
        payload.put("registrationDeadline", deadline);
        payload.put("ownerName", "Ops Owner");
        payload.put("contactPhone", "13800000000");
        payload.put("planContent", "Plan content");
        return payload;
    }

    private Map<String, Object> processPayload(String timeLabel, String title, int sortOrder) {
        return Map.of(
                "timeLabel", timeLabel,
                "title", title,
                "description", "Step description",
                "sortOrder", sortOrder
        );
    }

    private Map<String, Object> customFieldPayload(String fieldKey, String label, String fieldType, boolean required, int sortOrder) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fieldKey", fieldKey);
        payload.put("label", label);
        payload.put("fieldType", fieldType);
        payload.put("required", required);
        payload.put("options", List.of("A", "B"));
        payload.put("sortOrder", sortOrder);
        return payload;
    }

    private String login(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/admin/auth/login",
                new HttpEntity<>(Map.of("username", username, "password", "password123"), headers),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) data(response).get("token");
    }

    private ResponseEntity<Map> exchange(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                method,
                new HttpEntity<>(body, headers),
                Map.class
        );
    }

    private Map<?, ?> data(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (Map<?, ?>) response.getBody().get("data");
    }

    private List<?> dataList(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (List<?>) response.getBody().get("data");
    }
}
