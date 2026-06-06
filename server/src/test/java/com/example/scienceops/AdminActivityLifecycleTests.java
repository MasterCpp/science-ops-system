package com.example.scienceops;

import java.util.LinkedHashMap;
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
class AdminActivityLifecycleTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void activityCanBeCreatedAsDraftAndEditedWhileDraft() {
        String token = login("activityadmin");

        Map<?, ?> created = createActivity(token, "Draft Editable Activity", 80, "2026-07-01T18:00:00");
        assertThat(created.get("status")).isEqualTo("DRAFT");
        assertThat(created.get("capacity")).isEqualTo(80);
        assertThat(created.get("ownerName")).isEqualTo("Ops Owner");

        String id = (String) created.get("id");
        ResponseEntity<Map> updatedResponse = putActivity(
                token,
                id,
                activityPayload("Draft Editable Activity Updated", 120, "2026-07-02T18:00:00", "Updated Location")
        );

        assertThat(updatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> updated = data(updatedResponse);
        assertThat(updated.get("title")).isEqualTo("Draft Editable Activity Updated");
        assertThat(updated.get("capacity")).isEqualTo(120);
        assertThat(updated.get("location")).isEqualTo("Updated Location");

        ResponseEntity<Map> detailResponse = exchange(HttpMethod.GET, "/api/admin/activities/" + id, token, null);
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(detailResponse).get("status")).isEqualTo("DRAFT");
    }

    @Test
    void listSupportsKeywordStatusAndTimeRangeFilters() {
        String token = login("activityadmin");
        createActivity(token, "Keyword Galaxy Camp", 50, "2026-07-03T18:00:00");
        createActivity(token, "Other Workshop", 50, "2026-09-03T18:00:00");

        ResponseEntity<Map> response = exchange(
                HttpMethod.GET,
                "/api/admin/activities?keyword=Galaxy&status=DRAFT&startFrom=2026-07-01T00:00:00&startTo=2026-07-31T23:59:59&page=1&pageSize=10",
                token,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> page = data(response);
        assertThat(page.get("page")).isEqualTo(1);
        assertThat(page.get("pageSize")).isEqualTo(10);
        assertThat(page.get("total")).isEqualTo(1);
        assertThat(response.getBody().toString()).contains("Keyword Galaxy Camp");
        assertThat(response.getBody().toString()).doesNotContain("Other Workshop");
    }

    @Test
    void activityMovesThroughLifecycleAndArchivedActivityIsReadOnlyUntilSuperAdminUnarchives() {
        String activityToken = login("activityadmin");
        String superToken = login("superadmin");
        String id = (String) createActivity(activityToken, "Lifecycle Activity", 30, "2026-07-04T18:00:00").get("id");

        assertThat(postTransition(activityToken, id, "publish").get("status")).isEqualTo("REGISTRATION_OPEN");
        assertThat(postTransition(activityToken, id, "start").get("status")).isEqualTo("IN_PROGRESS");
        assertThat(postTransition(activityToken, id, "end").get("status")).isEqualTo("ENDED");
        assertThat(postTransition(activityToken, id, "archive").get("status")).isEqualTo("ARCHIVED");

        ResponseEntity<Map> archivedUpdate = putActivity(
                activityToken,
                id,
                activityPayload("Archived Update Rejected", 30, "2026-07-04T18:00:00", "Location")
        );
        assertThat(archivedUpdate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(archivedUpdate.getBody().get("code")).isEqualTo("INVALID_STATE");

        ResponseEntity<Map> activityAdminUnarchive = exchange(HttpMethod.POST, "/api/admin/activities/" + id + "/unarchive", activityToken, null);
        assertThat(activityAdminUnarchive.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(postTransition(superToken, id, "unarchive").get("status")).isEqualTo("ENDED");
    }

    @Test
    void inProgressActivityRejectsCapacityAndDeadlineChanges() {
        String token = login("activityadmin");
        String id = (String) createActivity(token, "In Progress Locked Activity", 20, "2026-07-05T18:00:00").get("id");
        postTransition(token, id, "publish");
        postTransition(token, id, "start");

        ResponseEntity<Map> response = putActivity(
                token,
                id,
                activityPayload("In Progress Locked Activity", 25, "2026-07-06T18:00:00", "Location")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_STATE");
    }

    @Test
    void activityAdminCannotDeleteButSuperAdminCanDeleteActivity() {
        String activityToken = login("activityadmin");
        String superToken = login("superadmin");
        String id = (String) createActivity(activityToken, "Delete Permission Activity", 10, "2026-07-06T18:00:00").get("id");

        ResponseEntity<Map> denied = exchange(HttpMethod.DELETE, "/api/admin/activities/" + id, activityToken, null);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getBody().get("code")).isEqualTo("FORBIDDEN");

        ResponseEntity<Map> deleted = exchange(HttpMethod.DELETE, "/api/admin/activities/" + id, superToken, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> detail = exchange(HttpMethod.GET, "/api/admin/activities/" + id, superToken, null);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(detail.getBody().get("code")).isEqualTo("NOT_FOUND");
    }

    private Map<?, ?> createActivity(String token, String title, Integer capacity, String deadline) {
        ResponseEntity<Map> response = exchange(
                HttpMethod.POST,
                "/api/admin/activities",
                token,
                activityPayload(title, capacity, deadline, "Location")
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return data(response);
    }

    private ResponseEntity<Map> putActivity(String token, String id, Map<String, Object> payload) {
        return exchange(HttpMethod.PUT, "/api/admin/activities/" + id, token, payload);
    }

    private Map<?, ?> postTransition(String token, String id, String action) {
        ResponseEntity<Map> response = exchange(HttpMethod.POST, "/api/admin/activities/" + id + "/" + action, token, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return data(response);
    }

    private Map<String, Object> activityPayload(String title, Integer capacity, String deadline, String location) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("coverFileId", 9001);
        payload.put("description", "Activity description");
        payload.put("startTime", "2026-07-01T09:00:00");
        payload.put("endTime", "2026-07-01T12:00:00");
        payload.put("location", location);
        payload.put("capacity", capacity);
        payload.put("registrationDeadline", deadline);
        payload.put("ownerName", "Ops Owner");
        payload.put("contactPhone", "13800000000");
        payload.put("planContent", "Plan content");
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
}
