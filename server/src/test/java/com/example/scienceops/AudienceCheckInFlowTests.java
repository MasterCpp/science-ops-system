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
class AudienceCheckInFlowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void publicCheckInSucceedsRejectsDuplicateAndAppearsInAdminListAndExport() {
        String token = login("activityadmin");
        String activityId = createPublishedActivity(token, "Public Check-in Activity");
        publicRegister(activityId, registrationPayload("Alice", "13900000001"));
        exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/start", token, null);

        ResponseEntity<Map> checkedIn = publicCheckIn(activityId, "13900000001");
        assertThat(checkedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> checkIn = data(checkedIn);
        assertThat(checkIn.get("activityTitle")).isEqualTo("Public Check-in Activity");
        assertThat(checkIn.get("name")).isEqualTo("Alice");
        assertThat(checkIn.get("method")).isEqualTo("QR");
        assertThat(checkIn.get("manual")).isEqualTo(false);
        assertThat(checkIn.get("checkInTime")).isNotNull();

        ResponseEntity<Map> duplicate = publicCheckIn(activityId, "13900000001");
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("ALREADY_CHECKED_IN");

        ResponseEntity<Map> list = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/check-ins?keyword=Alice", token, null);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(list).get("total")).isEqualTo(1);

        ResponseEntity<String> export = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/activities/" + activityId + "/check-ins/export",
                HttpMethod.GET,
                authorized(token),
                String.class
        );
        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(export.getBody()).contains("Name,Phone,Check-in Time,Method,Manual,Status");
        assertThat(export.getBody()).contains("Alice");
    }

    @Test
    void publicCheckInRejectsUnregisteredCancelledAndInvalidActivityStates() {
        String token = login("activityadmin");
        String openActivityId = createPublishedActivity(token, "Open Check-in Activity");
        Map<?, ?> registration = data(publicRegister(openActivityId, registrationPayload("Bob", "13900000002")));

        ResponseEntity<Map> notStarted = publicCheckIn(openActivityId, "13900000002");
        assertThat(notStarted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(notStarted.getBody().get("code")).isEqualTo("INVALID_STATE");

        exchange(HttpMethod.POST, "/api/admin/activities/" + openActivityId + "/start", token, null);
        ResponseEntity<Map> missing = publicCheckIn(openActivityId, "13900009999");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody().get("code")).isEqualTo("NOT_FOUND");

        exchange(HttpMethod.POST, "/api/admin/registrations/" + registration.get("id") + "/cancel", token, null);
        ResponseEntity<Map> cancelled = publicCheckIn(openActivityId, "13900000002");
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(cancelled.getBody().get("code")).isEqualTo("INVALID_STATE");

        String endedActivityId = createPublishedActivity(token, "Ended Check-in Activity");
        publicRegister(endedActivityId, registrationPayload("Cara", "13900000003"));
        exchange(HttpMethod.POST, "/api/admin/activities/" + endedActivityId + "/start", token, null);
        exchange(HttpMethod.POST, "/api/admin/activities/" + endedActivityId + "/end", token, null);
        ResponseEntity<Map> ended = publicCheckIn(endedActivityId, "13900000003");
        assertThat(ended.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ended.getBody().get("code")).isEqualTo("INVALID_STATE");
    }

    @Test
    void adminCanManualCheckInRevokeAndRevokedRecordsAreNotActive() {
        String token = login("activityadmin");
        String activityId = createPublishedActivity(token, "Manual Check-in Activity");
        Map<?, ?> registration = data(publicRegister(activityId, registrationPayload("Dora", "13900000004")));
        exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/start", token, null);

        ResponseEntity<Map> manual = exchange(
                HttpMethod.POST,
                "/api/admin/activities/" + activityId + "/check-ins/manual",
                token,
                Map.of("registrationId", registration.get("id"), "checkInTime", "2026-11-01T09:30:00")
        );
        assertThat(manual.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> manualData = data(manual);
        assertThat(manualData.get("method")).isEqualTo("MANUAL");
        assertThat(manualData.get("manual")).isEqualTo(true);
        assertThat(manualData.get("status")).isEqualTo("CHECKED_IN");

        ResponseEntity<Map> activityBeforeRevoke = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId, token, null);
        assertThat(data(activityBeforeRevoke).get("checkedInCount")).isEqualTo(1);

        ResponseEntity<Map> revoked = exchange(HttpMethod.POST, "/api/admin/check-ins/" + manualData.get("id") + "/revoke", token, null);
        assertThat(revoked.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(revoked).get("status")).isEqualTo("REVOKED");

        ResponseEntity<Map> activeList = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/check-ins?status=CHECKED_IN", token, null);
        assertThat(data(activeList).get("total")).isEqualTo(0);

        ResponseEntity<Map> activityAfterRevoke = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId, token, null);
        assertThat(data(activityAfterRevoke).get("checkedInCount")).isEqualTo(0);
    }

    @Test
    void publicActivityDetailIncludesFixedRegistrationAndCheckInLinks() {
        String token = login("activityadmin");
        String activityId = createPublishedActivity(token, "Link Activity");

        ResponseEntity<Map> detail = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/mobile/activities/" + activityId,
                Map.class
        );
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(detail).get("registrationLink")).isEqualTo("/m/activities/" + activityId);
        assertThat(data(detail).get("checkInLink")).isEqualTo("/m/activities/" + activityId + "/check-in");
    }

    private String createPublishedActivity(String token, String title) {
        String id = (String) data(exchange(HttpMethod.POST, "/api/admin/activities", token, activityPayload(title))).get("id");
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

    private ResponseEntity<Map> publicCheckIn(String activityId, String phone) {
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/api/mobile/activities/" + activityId + "/check-ins",
                json(Map.of("phone", phone)),
                Map.class
        );
    }

    private Map<String, Object> activityPayload(String title) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", "Activity description");
        payload.put("startTime", "2026-11-01T09:00:00");
        payload.put("endTime", "2026-11-01T12:00:00");
        payload.put("location", "Science Hall");
        payload.put("capacity", 20);
        payload.put("registrationDeadline", "2026-12-01T18:00:00");
        payload.put("ownerName", "Ops Owner");
        payload.put("contactPhone", "13800000000");
        payload.put("planContent", "Plan content");
        return payload;
    }

    private Map<String, Object> registrationPayload(String name, String phone) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("phone", phone);
        payload.put("attendeeCount", 1);
        payload.put("unitName", "Science School");
        payload.put("ageGroup", "Student");
        payload.put("remark", "Remark");
        payload.put("customValues", List.of());
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
