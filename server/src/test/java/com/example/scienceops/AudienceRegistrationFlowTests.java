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
class AudienceRegistrationFlowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void publicRegistrationSucceedsAndRejectsDuplicatePhone() {
        String token = login("activityadmin");
        String activityId = createPublishedActivity(token, "Public Registration Activity", 3, "2026-12-01T18:00:00");
        post(token, "/api/admin/activities/" + activityId + "/custom-fields", customFieldPayload("grade", "Grade"));

        ResponseEntity<Map> first = publicRegister(activityId, registrationPayload("Alice", "13800000001", 2));
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = data(first);
        assertThat(data.get("activityTitle")).isEqualTo("Public Registration Activity");
        assertThat(data.get("name")).isEqualTo("Alice");
        assertThat(data.get("attendeeCount")).isEqualTo(2);
        assertThat(first.getBody().toString()).contains("Grade");

        ResponseEntity<Map> duplicate = publicRegister(activityId, registrationPayload("Alice Again", "13800000001", 1));
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("DUPLICATE_SUBMISSION");
    }

    @Test
    void publicRegistrationRejectsDeadlineAndCapacityFailures() {
        String token = login("activityadmin");
        String expiredId = createPublishedActivity(token, "Expired Registration Activity", 5, "2020-01-01T18:00:00");
        String fullId = createPublishedActivity(token, "Full Registration Activity", 1, "2026-12-01T18:00:00");

        ResponseEntity<Map> expired = publicRegister(expiredId, registrationPayload("Bob", "13800000002", 1));
        assertThat(expired.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(expired.getBody().get("code")).isEqualTo("DEADLINE_PASSED");

        ResponseEntity<Map> full = publicRegister(fullId, registrationPayload("Carol", "13800000003", 2));
        assertThat(full.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(full.getBody().get("code")).isEqualTo("CAPACITY_FULL");
    }

    @Test
    void adminCanListBackfillCancelAndExportRegistrationsAndCancellationFreesCapacity() {
        String token = login("activityadmin");
        String activityId = createPublishedActivity(token, "Admin Registration Activity", 2, "2026-12-01T18:00:00");
        Map<?, ?> registration = data(publicRegister(activityId, registrationPayload("Dora", "13800000004", 2)));
        String registrationId = (String) registration.get("id");

        ResponseEntity<Map> list = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/registrations?keyword=Dora", token, null);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(list).get("total")).isEqualTo(1);

        ResponseEntity<Map> capacityFull = publicRegister(activityId, registrationPayload("Evan", "13800000005", 1));
        assertThat(capacityFull.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(capacityFull.getBody().get("code")).isEqualTo("CAPACITY_FULL");

        ResponseEntity<Map> cancelled = exchange(HttpMethod.POST, "/api/admin/registrations/" + registrationId + "/cancel", token, null);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(cancelled).get("status")).isEqualTo("CANCELLED");

        ResponseEntity<Map> afterCancel = publicRegister(activityId, registrationPayload("Evan", "13800000005", 1));
        assertThat(afterCancel.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> backfill = exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/registrations", token, registrationPayload("Fay", "13800000006", 1));
        assertThat(backfill.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(backfill).get("name")).isEqualTo("Fay");

        ResponseEntity<String> export = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/activities/" + activityId + "/registrations/export",
                HttpMethod.GET,
                authorized(token),
                String.class
        );
        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(export.getBody()).contains("Name,Phone,Attendee Count");
        assertThat(export.getBody()).contains("Dora");
        assertThat(export.getBody()).contains("Evan");
    }

    private String createPublishedActivity(String token, String title, Integer capacity, String deadline) {
        String id = (String) data(exchange(HttpMethod.POST, "/api/admin/activities", token, activityPayload(title, capacity, deadline))).get("id");
        exchange(HttpMethod.POST, "/api/admin/activities/" + id + "/publish", token, null);
        return id;
    }

    private ResponseEntity<Map> publicRegister(String activityId, Map<String, Object> payload) {
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/api/mobile/activities/" + activityId + "/registrations",
                json(payload),
                Map.class
        );
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

    private Map<String, Object> customFieldPayload(String fieldKey, String label) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fieldKey", fieldKey);
        payload.put("label", label);
        payload.put("fieldType", "TEXT");
        payload.put("required", false);
        payload.put("options", List.of());
        payload.put("sortOrder", 1);
        return payload;
    }

    private Map<String, Object> registrationPayload(String name, String phone, int attendeeCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("phone", phone);
        payload.put("attendeeCount", attendeeCount);
        payload.put("unitName", "Science School");
        payload.put("ageGroup", "Student");
        payload.put("remark", "Remark");
        payload.put("customValues", List.of(Map.of("fieldKey", "grade", "value", "Grade 3")));
        return payload;
    }

    private String login(String username) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/admin/auth/login",
                json(Map.of("username", username, "password", "password123")),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) data(response).get("token");
    }

    private ResponseEntity<Map> post(String token, String path, Object payload) {
        ResponseEntity<Map> response = exchange(HttpMethod.POST, path, token, payload);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response;
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

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private Map<?, ?> data(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (Map<?, ?>) response.getBody().get("data");
    }
}
