package com.example.scienceops;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "science-ops.storage.local-path=./target/test-storage"
)
class OperationLogAuditFlowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void auditedAdminActionsCreateQueryableOperationLogs() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String superToken = login("superadmin");
        String title = "Audit Trail Activity " + System.nanoTime();

        String activityId = (String) data(exchange(HttpMethod.POST, "/api/admin/activities", activityToken, activityPayload(title), Map.class)).get("id");
        exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/publish", activityToken, null, Map.class);
        download("/api/admin/activities/" + activityId + "/registrations/export", activityToken);

        String registrationId = (String) data(exchange(
                HttpMethod.POST,
                "/api/admin/activities/" + activityId + "/registrations",
                activityToken,
                registrationPayload(),
                Map.class
        )).get("id");
        exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/start", activityToken, null, Map.class);
        String checkInId = (String) data(exchange(
                HttpMethod.POST,
                "/api/admin/activities/" + activityId + "/check-ins/manual",
                activityToken,
                Map.of("registrationId", registrationId, "checkInTime", "2026-12-01T09:10:00"),
                Map.class
        )).get("id");
        exchange(HttpMethod.POST, "/api/admin/check-ins/" + checkInId + "/revoke", activityToken, null, Map.class);

        String positionId = (String) data(exchange(
                HttpMethod.POST,
                "/api/admin/activities/" + activityId + "/volunteer-positions",
                volunteerToken,
                volunteerPositionPayload(),
                Map.class
        )).get("id");
        String applicationId = (String) data(exchange(
                HttpMethod.POST,
                "/api/mobile/activities/" + activityId + "/volunteer-applications",
                null,
                volunteerApplicationPayload(positionId),
                Map.class
        )).get("id");
        exchange(
                HttpMethod.POST,
                "/api/admin/volunteer-applications/" + applicationId + "/approve",
                volunteerToken,
                Map.of("reviewNote", "Approved for audit test"),
                Map.class
        );

        String fileId = (String) data(upload(activityToken, activityId, "ATTACHMENT", "audit-plan.pdf", "%PDF-1.4".getBytes())).get("id");
        exchange(HttpMethod.DELETE, "/api/admin/files/" + fileId, activityToken, null, Map.class);

        Map<?, ?> createLog = findLog(superToken, "ACTIVITY_CREATE", "ACTIVITY", title);
        assertThat(createLog.get("adminUsername")).isEqualTo("activityadmin");
        assertThat(createLog.get("targetId")).isEqualTo(activityId);
        assertThat(createLog.get("ip")).isNotNull();

        assertThat(findLog(superToken, "ACTIVITY_PUBLISH", "ACTIVITY", title)).isNotNull();
        assertThat(findLog(superToken, "REGISTRATION_EXPORT", "REGISTRATION", "Activity registrations")).isNotNull();
        assertThat(findLog(superToken, "REGISTRATION_BACKFILL", "REGISTRATION", "Audit User")).isNotNull();
        assertThat(findLog(superToken, "CHECK_IN_MANUAL", "CHECK_IN", "Audit User")).isNotNull();
        assertThat(findLog(superToken, "CHECK_IN_REVOKE", "CHECK_IN", "Audit User")).isNotNull();
        assertThat(findLog(superToken, "VOLUNTEER_APPLICATION_APPROVE", "VOLUNTEER_APPLICATION", "Audit Volunteer")).isNotNull();
        assertThat(findLog(superToken, "FILE_DELETE", "FILE_ASSET", "audit-plan.pdf")).isNotNull();

        String activityAdminId = (String) createLog.get("adminUserId");
        ResponseEntity<Map> filtered = exchange(
                HttpMethod.GET,
                "/api/admin/operation-logs?adminUserId=" + activityAdminId + "&action=ACTIVITY_CREATE&targetType=ACTIVITY&page=1&pageSize=5",
                superToken,
                null,
                Map.class
        );
        assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(items(filtered)).anySatisfy(item -> assertThat(item.get("targetSummary")).isEqualTo(title));

        ResponseEntity<Map> detail = exchange(HttpMethod.GET, "/api/admin/operation-logs/" + createLog.get("id"), superToken, null, Map.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(detail).get("action")).isEqualTo("ACTIVITY_CREATE");
        assertThat((String) data(detail).get("detailJson")).contains("DRAFT");
    }

    @Test
    void operationLogsAreVisibleOnlyToSuperAdmin() {
        String superToken = login("superadmin");
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");

        assertThat(exchange(HttpMethod.GET, "/api/admin/operation-logs", superToken, null, Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exchange(HttpMethod.GET, "/api/admin/operation-logs", activityToken, null, Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange(HttpMethod.GET, "/api/admin/operation-logs", volunteerToken, null, Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Map<?, ?> findLog(String token, String action, String targetType, String targetSummary) {
        ResponseEntity<Map> response = exchange(
                HttpMethod.GET,
                "/api/admin/operation-logs?action=" + action + "&targetType=" + targetType + "&page=1&pageSize=100",
                token,
                null,
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return items(response).stream()
                .filter(item -> targetSummary.equals(item.get("targetSummary")))
                .findFirst()
                .orElseThrow();
    }

    private ResponseEntity<Map> upload(String token, String activityId, String category, String filename, byte[] bytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("category", category);
        body.add("file", new NamedByteArrayResource(bytes, filename));
        return restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/activities/" + activityId + "/files",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
    }

    private byte[] download(String path, String token) {
        ResponseEntity<byte[]> response = restTemplate.exchange(
                "http://localhost:" + port + path,
                HttpMethod.GET,
                authorized(token),
                byte[].class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
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

    private <T> ResponseEntity<T> exchange(HttpMethod method, String path, String token, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
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

    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private Map<String, Object> activityPayload(String title) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("coverFileId", null);
        payload.put("description", "Activity description");
        payload.put("startTime", "2026-12-01T09:00:00");
        payload.put("endTime", "2026-12-01T12:00:00");
        payload.put("location", "Audit Hall");
        payload.put("capacity", 50);
        payload.put("registrationDeadline", "2026-11-30T18:00:00");
        payload.put("ownerName", "Ops Owner");
        payload.put("contactPhone", "13800000000");
        payload.put("planContent", "Plan content");
        return payload;
    }

    private Map<String, Object> registrationPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "Audit User");
        payload.put("phone", "13900000001");
        payload.put("attendeeCount", 1);
        payload.put("unitName", "Audit School");
        payload.put("ageGroup", "ADULT");
        payload.put("remark", "Audit registration");
        payload.put("customValues", List.of());
        return payload;
    }

    private Map<String, Object> volunteerPositionPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "Audit Guide");
        payload.put("description", "Guide visitors");
        payload.put("capacity", 3);
        payload.put("serviceStartTime", "2026-12-01T08:30:00");
        payload.put("serviceEndTime", "2026-12-01T12:30:00");
        return payload;
    }

    private Map<String, Object> volunteerApplicationPayload(String positionId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("positionId", positionId);
        payload.put("name", "Audit Volunteer");
        payload.put("phone", "13900000002");
        payload.put("unitName", "Audit Unit");
        payload.put("ageGroup", "ADULT");
        payload.put("availableTimeNote", "All morning");
        payload.put("experienceNote", "Experienced");
        payload.put("remark", "Audit volunteer");
        return payload;
    }

    private Map<?, ?> data(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (Map<?, ?>) response.getBody().get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<?, ?>> items(ResponseEntity<Map> response) {
        return (List<Map<?, ?>>) data(response).get("items");
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
