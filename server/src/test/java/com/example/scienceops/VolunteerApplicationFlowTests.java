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
class VolunteerApplicationFlowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void adminCanCreateUpdateListAndDeleteVolunteerPositions() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createPublishedActivity(activityToken, "Volunteer Position CRUD Activity");

        ResponseEntity<Map> created = exchange(
                HttpMethod.POST,
                "/api/admin/activities/" + activityId + "/volunteer-positions",
                volunteerToken,
                positionPayload("Guide", 2)
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> position = data(created);
        assertThat(position.get("name")).isEqualTo("Guide");
        assertThat(position.get("capacity")).isEqualTo(2);
        assertThat(position.get("approvedCount")).isEqualTo(0);
        String positionId = (String) position.get("id");

        ResponseEntity<Map> updated = exchange(
                HttpMethod.PUT,
                "/api/admin/volunteer-positions/" + positionId,
                volunteerToken,
                positionPayload("Entrance Guide", 3)
        );
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(updated).get("name")).isEqualTo("Entrance Guide");
        assertThat(data(updated).get("capacity")).isEqualTo(3);

        ResponseEntity<Map> list = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/volunteer-positions", volunteerToken, null);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<?>) rawData(list)).size()).isEqualTo(1);

        ResponseEntity<Map> deleted = exchange(HttpMethod.DELETE, "/api/admin/volunteer-positions/" + positionId, volunteerToken, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> afterDelete = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/volunteer-positions", volunteerToken, null);
        assertThat(((List<?>) rawData(afterDelete)).size()).isEqualTo(0);
    }

    @Test
    void publicApplicationSucceedsRejectsDuplicateAndListsPublicPositions() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createPublishedActivity(activityToken, "Volunteer Public Activity");
        String positionId = createPosition(volunteerToken, activityId, "Support", 2);

        ResponseEntity<Map> publicPositions = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/mobile/activities/" + activityId + "/volunteer-positions",
                Map.class
        );
        assertThat(publicPositions.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> positions = (List<?>) rawData(publicPositions);
        assertThat(positions).hasSize(1);
        assertThat(String.valueOf(positions.get(0))).contains("Support", "approvedCount=0");

        ResponseEntity<Map> first = publicApply(activityId, applicationPayload(positionId, "Alice", "13700000001"));
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> application = data(first);
        assertThat(application.get("positionName")).isEqualTo("Support");
        assertThat(application.get("status")).isEqualTo("PENDING");

        ResponseEntity<Map> duplicate = publicApply(activityId, applicationPayload(positionId, "Alice Again", "13700000001"));
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("DUPLICATE_SUBMISSION");
    }

    @Test
    void approvalOccupiesCapacityAndFullPositionRejectsNewApplications() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createPublishedActivity(activityToken, "Volunteer Capacity Activity");
        String positionId = createPosition(volunteerToken, activityId, "Capacity Role", 1);
        String applicationId = (String) data(publicApply(activityId, applicationPayload(positionId, "Bob", "13700000002"))).get("id");

        ResponseEntity<Map> approved = exchange(
                HttpMethod.POST,
                "/api/admin/volunteer-applications/" + applicationId + "/approve",
                volunteerToken,
                Map.of("reviewNote", "Approved")
        );
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(approved).get("status")).isEqualTo("APPROVED");

        ResponseEntity<Map> positions = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/volunteer-positions", volunteerToken, null);
        assertThat(String.valueOf(rawData(positions))).contains("approvedCount=1", "full=true");

        ResponseEntity<Map> full = publicApply(activityId, applicationPayload(positionId, "Cara", "13700000003"));
        assertThat(full.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(full.getBody().get("code")).isEqualTo("CAPACITY_FULL");
    }

    @Test
    void adminCanRejectCancelListAndExportVolunteerApplications() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createPublishedActivity(activityToken, "Volunteer Review Activity");
        String positionId = createPosition(volunteerToken, activityId, "Review Role", 3);
        String rejectedId = (String) data(publicApply(activityId, applicationPayload(positionId, "Dora", "13700000004"))).get("id");
        String cancelledId = (String) data(publicApply(activityId, applicationPayload(positionId, "Evan", "13700000005"))).get("id");

        ResponseEntity<Map> rejected = exchange(
                HttpMethod.POST,
                "/api/admin/volunteer-applications/" + rejectedId + "/reject",
                volunteerToken,
                Map.of("reviewNote", "No matching time")
        );
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(rejected).get("status")).isEqualTo("REJECTED");
        assertThat(data(rejected).get("reviewNote")).isEqualTo("No matching time");

        ResponseEntity<Map> cancelled = exchange(
                HttpMethod.POST,
                "/api/admin/volunteer-applications/" + cancelledId + "/cancel",
                volunteerToken,
                Map.of("reviewNote", "Cancelled by admin")
        );
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(cancelled).get("status")).isEqualTo("CANCELLED");

        ResponseEntity<Map> list = exchange(HttpMethod.GET, "/api/admin/volunteer-applications?activityId=" + activityId + "&keyword=Dora", volunteerToken, null);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(list).get("total")).isEqualTo(1);

        ResponseEntity<String> export = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/volunteer-applications/export?activityId=" + activityId,
                HttpMethod.GET,
                authorized(volunteerToken),
                String.class
        );
        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(export.getBody()).contains("Activity,Position,Name,Phone,Unit/School,Age Group,Status");
        assertThat(export.getBody()).contains("Dora");
        assertThat(export.getBody()).contains("Evan");
    }

    @Test
    void publicActivityDetailIncludesVolunteerLink() {
        String token = login("activityadmin");
        String activityId = createPublishedActivity(token, "Volunteer Link Activity");

        ResponseEntity<Map> detail = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/mobile/activities/" + activityId,
                Map.class
        );
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(detail).get("volunteerLink")).isEqualTo("/m/activities/" + activityId + "/volunteers");
    }

    @Test
    void volunteerMustBeApprovedBeforeCheckInAndCanCheckInAndCheckOutOnce() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createPublishedActivity(activityToken, "Volunteer Attendance Activity");
        String positionId = createPosition(volunteerToken, activityId, "Service Role", 2);
        String applicationId = (String) data(publicApply(activityId, applicationPayload(positionId, "Fay", "13700000006"))).get("id");

        ResponseEntity<Map> beforeApproval = publicVolunteerCheckIn(activityId, applicationId);
        assertThat(beforeApproval.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(beforeApproval.getBody().get("code")).isEqualTo("NOT_APPROVED");

        exchange(HttpMethod.POST, "/api/admin/volunteer-applications/" + applicationId + "/approve", volunteerToken, Map.of("reviewNote", "Approved"));

        ResponseEntity<Map> checkedIn = publicVolunteerCheckIn(activityId, applicationId);
        assertThat(checkedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> checkIn = data(checkedIn);
        assertThat(checkIn.get("status")).isEqualTo("CHECKED_IN");
        assertThat(checkIn.get("checkInTime")).isNotNull();

        ResponseEntity<Map> duplicateCheckIn = publicVolunteerCheckIn(activityId, applicationId);
        assertThat(duplicateCheckIn.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateCheckIn.getBody().get("code")).isEqualTo("ALREADY_CHECKED_IN");

        ResponseEntity<Map> status = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/mobile/activities/" + activityId + "/volunteer-applications/status",
                json(Map.of("phone", "13700000006")),
                Map.class
        );
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(String.valueOf(data(status))).contains("Fay", "CHECKED_IN");

        ResponseEntity<Map> checkedOut = publicVolunteerCheckOut(activityId, applicationId);
        assertThat(checkedOut.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(checkedOut).get("status")).isEqualTo("CHECKED_OUT");
        assertThat((Integer) data(checkedOut).get("serviceMinutes")).isGreaterThanOrEqualTo(0);

        ResponseEntity<Map> duplicateCheckOut = publicVolunteerCheckOut(activityId, applicationId);
        assertThat(duplicateCheckOut.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateCheckOut.getBody().get("code")).isEqualTo("ALREADY_CHECKED_OUT");
    }

    @Test
    void adminCanManualCheckInManualCheckOutAdjustAndRevokeAttendance() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createPublishedActivity(activityToken, "Volunteer Manual Attendance Activity");
        String positionId = createPosition(volunteerToken, activityId, "Manual Role", 2);
        String applicationId = (String) data(publicApply(activityId, applicationPayload(positionId, "Gina", "13700000007"))).get("id");
        exchange(HttpMethod.POST, "/api/admin/volunteer-applications/" + applicationId + "/approve", volunteerToken, Map.of("reviewNote", "Approved"));

        ResponseEntity<Map> manualIn = exchange(
                HttpMethod.POST,
                "/api/admin/activities/" + activityId + "/volunteer-attendance/manual-check-in",
                volunteerToken,
                Map.of("applicationId", applicationId, "checkInTime", "2026-11-01T08:00:00")
        );
        assertThat(manualIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(manualIn).get("status")).isEqualTo("CHECKED_IN");
        String attendanceId = (String) data(manualIn).get("id");

        ResponseEntity<Map> manualOut = exchange(
                HttpMethod.POST,
                "/api/admin/volunteer-applications/" + applicationId + "/attendance/manual-check-out",
                volunteerToken,
                Map.of("checkOutTime", "2026-11-01T10:30:00")
        );
        assertThat(manualOut.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(manualOut).get("status")).isEqualTo("CHECKED_OUT");
        assertThat(data(manualOut).get("serviceMinutes")).isEqualTo(150);
        assertThat(data(manualOut).get("effectiveServiceMinutes")).isEqualTo(150);

        ResponseEntity<Map> adjusted = exchange(
                HttpMethod.POST,
                "/api/admin/volunteer-attendance/" + attendanceId + "/adjust",
                volunteerToken,
                Map.of("serviceMinutes", 180, "adjustmentReason", "Extra preparation time")
        );
        assertThat(adjusted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(adjusted).get("manuallyAdjusted")).isEqualTo(true);
        assertThat(data(adjusted).get("effectiveServiceMinutes")).isEqualTo(180);

        ResponseEntity<Map> list = exchange(HttpMethod.GET, "/api/admin/volunteer-attendance?activityId=" + activityId + "&keyword=Gina", volunteerToken, null);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(list).get("total")).isEqualTo(1);

        ResponseEntity<Map> revoked = exchange(HttpMethod.POST, "/api/admin/volunteer-attendance/" + attendanceId + "/revoke", volunteerToken, null);
        assertThat(revoked.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(revoked).get("status")).isEqualTo("REVOKED");
        assertThat(data(revoked).get("effectiveServiceMinutes")).isEqualTo(0);
    }

    private String createPublishedActivity(String token, String title) {
        String id = (String) data(exchange(HttpMethod.POST, "/api/admin/activities", token, activityPayload(title))).get("id");
        exchange(HttpMethod.POST, "/api/admin/activities/" + id + "/publish", token, null);
        return id;
    }

    private String createPosition(String token, String activityId, String name, int capacity) {
        return (String) data(exchange(
                HttpMethod.POST,
                "/api/admin/activities/" + activityId + "/volunteer-positions",
                token,
                positionPayload(name, capacity)
        )).get("id");
    }

    private ResponseEntity<Map> publicApply(String activityId, Map<String, Object> payload) {
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/api/mobile/activities/" + activityId + "/volunteer-applications",
                json(payload),
                Map.class
        );
    }

    private ResponseEntity<Map> publicVolunteerCheckIn(String activityId, String applicationId) {
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/api/mobile/activities/" + activityId + "/volunteer-applications/" + applicationId + "/check-in",
                null,
                Map.class
        );
    }

    private ResponseEntity<Map> publicVolunteerCheckOut(String activityId, String applicationId) {
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/api/mobile/activities/" + activityId + "/volunteer-applications/" + applicationId + "/check-out",
                null,
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

    private Map<String, Object> positionPayload(String name, int capacity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", "Volunteer work description");
        payload.put("capacity", capacity);
        payload.put("serviceStartTime", "2026-11-01T08:30:00");
        payload.put("serviceEndTime", "2026-11-01T12:30:00");
        return payload;
    }

    private Map<String, Object> applicationPayload(String positionId, String name, String phone) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("positionId", positionId);
        payload.put("name", name);
        payload.put("phone", phone);
        payload.put("unitName", "Science School");
        payload.put("ageGroup", "Student");
        payload.put("availableTimeNote", "Morning available");
        payload.put("experienceNote", "Museum volunteer");
        payload.put("remark", "Remark");
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

    private Object rawData(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return response.getBody().get("data");
    }
}
