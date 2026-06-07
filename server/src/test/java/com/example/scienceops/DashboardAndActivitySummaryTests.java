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
        properties = "science-ops.storage.local-path=./target/test-storage-dashboard"
)
class DashboardAndActivitySummaryTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void activityDetailShowsCrossModuleSummaryMetricsAndExcludesInactiveRecords() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createPublishedActivity(activityToken, "Summary Activity");

        Map<?, ?> registration = data(publicRegister(activityId, registrationPayload("Alice", "13820000001")));
        Map<?, ?> cancelled = data(publicRegister(activityId, registrationPayload("Bob", "13820000002")));
        exchange(HttpMethod.POST, "/api/admin/registrations/" + cancelled.get("id") + "/cancel", activityToken, null);
        Map<?, ?> checkIn = adminCheckIn(activityToken, activityId, (String) registration.get("id"));
        Map<?, ?> revokedCheckInRegistration = data(publicRegister(activityId, registrationPayload("Cara", "13820000003")));
        Map<?, ?> revokedCheckIn = adminCheckIn(activityToken, activityId, (String) revokedCheckInRegistration.get("id"));
        exchange(HttpMethod.POST, "/api/admin/check-ins/" + revokedCheckIn.get("id") + "/revoke", activityToken, null);

        String positionId = createVolunteerPosition(volunteerToken, activityId);
        String approvedApplicationId = submitVolunteerApplication(activityId, positionId, "Vera", "13820010001");
        String pendingApplicationId = submitVolunteerApplication(activityId, positionId, "Will", "13820010002");
        exchange(HttpMethod.POST, "/api/admin/volunteer-applications/" + approvedApplicationId + "/approve", volunteerToken, Map.of("reviewNote", "ok"));
        Map<?, ?> attendance = data(exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/volunteer-attendance/manual-check-in", volunteerToken, Map.of("applicationId", approvedApplicationId, "checkInTime", "2026-11-01T09:00:00")));
        exchange(HttpMethod.POST, "/api/admin/volunteer-applications/" + approvedApplicationId + "/attendance/manual-check-out", volunteerToken, Map.of("checkOutTime", "2026-11-01T11:00:00"));

        String revokedApplicationId = submitVolunteerApplication(activityId, positionId, "Xena", "13820010003");
        exchange(HttpMethod.POST, "/api/admin/volunteer-applications/" + revokedApplicationId + "/approve", volunteerToken, Map.of("reviewNote", "ok"));
        Map<?, ?> revokedAttendance = data(exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/volunteer-attendance/manual-check-in", volunteerToken, Map.of("applicationId", revokedApplicationId, "checkInTime", "2026-11-01T09:00:00")));
        exchange(HttpMethod.POST, "/api/admin/volunteer-applications/" + revokedApplicationId + "/attendance/manual-check-out", volunteerToken, Map.of("checkOutTime", "2026-11-01T12:00:00"));
        exchange(HttpMethod.POST, "/api/admin/volunteer-attendance/" + revokedAttendance.get("id") + "/revoke", volunteerToken, null);

        String surveyId = createAndPublishSurvey(activityToken, activityId);
        submitSurvey(activityId, "13820000001", surveyId);

        String photoId = uploadPhoto(activityToken, activityId, "kept.jpg");
        String deletedPhotoId = uploadPhoto(activityToken, activityId, "deleted.jpg");
        exchange(HttpMethod.DELETE, "/api/admin/files/" + deletedPhotoId, activityToken, null);

        ResponseEntity<Map> detail = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId, activityToken, null);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> summary = data(detail);
        assertThat(summary.get("registeredAttendeeCount")).isEqualTo(2);
        assertThat(summary.get("checkedInCount")).isEqualTo(1);
        assertThat(String.valueOf(summary.get("checkInRate"))).startsWith("0.5");
        assertThat(summary.get("volunteerApplicationCount")).isEqualTo(3);
        assertThat(summary.get("approvedVolunteerCount")).isEqualTo(2);
        assertThat(summary.get("totalServiceMinutes")).isEqualTo(120);
        assertThat(summary.get("surveyResponseCount")).isEqualTo(1);
        assertThat(String.valueOf(summary.get("averageRating"))).startsWith("5");
        assertThat(summary.get("photoCount")).isEqualTo(1);
    }

    @Test
    void dashboardSummaryAndEntriesAreRoleAware() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createPublishedActivity(activityToken, "Dashboard Activity");
        publicRegister(activityId, registrationPayload("Dora", "13821000001"));
        String positionId = createVolunteerPosition(volunteerToken, activityId);
        submitVolunteerApplication(activityId, positionId, "Pending Volunteer", "13821010001");

        ResponseEntity<Map> activitySummary = exchange(HttpMethod.GET, "/api/admin/dashboard/summary", activityToken, null);
        assertThat(activitySummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) data(activitySummary).get("registrationCount")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat(data(activitySummary).get("volunteerApplicationCount")).isEqualTo(0);
        assertThat((Map<?, ?>) data(activitySummary).get("activityCountByStatus")).isNotEmpty();

        ResponseEntity<Map> volunteerSummary = exchange(HttpMethod.GET, "/api/admin/dashboard/summary", volunteerToken, null);
        assertThat(volunteerSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(volunteerSummary).get("registrationCount")).isEqualTo(0);
        assertThat(((Number) data(volunteerSummary).get("volunteerApplicationCount")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat((Map<?, ?>) data(volunteerSummary).get("activityCountByStatus")).isEmpty();

        ResponseEntity<Map> upcomingForActivityAdmin = exchange(HttpMethod.GET, "/api/admin/dashboard/upcoming-activities", activityToken, null);
        assertThat(upcomingForActivityAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dataList(upcomingForActivityAdmin)).isNotEmpty();

        ResponseEntity<Map> upcomingForVolunteerAdmin = exchange(HttpMethod.GET, "/api/admin/dashboard/upcoming-activities", volunteerToken, null);
        assertThat(upcomingForVolunteerAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dataList(upcomingForVolunteerAdmin)).isEmpty();

        ResponseEntity<Map> pendingForVolunteerAdmin = exchange(HttpMethod.GET, "/api/admin/dashboard/pending-volunteer-applications", volunteerToken, null);
        assertThat(pendingForVolunteerAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pendingForVolunteerAdmin.getBody().toString()).contains("Guide").contains("1");

        ResponseEntity<Map> pendingForActivityAdmin = exchange(HttpMethod.GET, "/api/admin/dashboard/pending-volunteer-applications", activityToken, null);
        assertThat(pendingForActivityAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dataList(pendingForActivityAdmin)).isEmpty();
    }

    private String createAndPublishSurvey(String token, String activityId) {
        String surveyId = (String) data(exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/survey", token, Map.of("title", "Survey", "description", "Desc"))).get("id");
        String ratingId = (String) data(exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/questions", token, questionPayload("Rating", "RATING", true, 10))).get("id");
        exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/publish", token, null);
        return surveyId + ":" + ratingId;
    }

    private void submitSurvey(String activityId, String phone, String survey) {
        String ratingId = survey.split(":")[1];
        ResponseEntity<Map> response = exchange(
                HttpMethod.POST,
                "/api/mobile/activities/" + activityId + "/survey/responses",
                null,
                Map.of("phone", phone, "answers", List.of(Map.of("questionId", ratingId, "numericValue", 5)))
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Map<?, ?> adminCheckIn(String token, String activityId, String registrationId) {
        ResponseEntity<Map> response = exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/check-ins/manual", token, Map.of("registrationId", registrationId, "checkInTime", "2026-11-01T09:30:00"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return data(response);
    }

    private String createVolunteerPosition(String token, String activityId) {
        ResponseEntity<Map> response = exchange(
                HttpMethod.POST,
                "/api/admin/activities/" + activityId + "/volunteer-positions",
                token,
                Map.of("name", "Guide", "capacity", 10, "serviceStartTime", "2026-11-01T09:00:00", "serviceEndTime", "2026-11-01T12:00:00", "description", "Guide visitors")
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) data(response).get("id");
    }

    private String submitVolunteerApplication(String activityId, String positionId, String name, String phone) {
        ResponseEntity<Map> response = exchange(
                HttpMethod.POST,
                "/api/mobile/activities/" + activityId + "/volunteer-applications",
                null,
                Map.of("positionId", positionId, "name", name, "phone", phone, "unitName", "School", "ageGroup", "Adult", "availableTimeNote", "All day", "experienceNote", "None", "remark", "Remark")
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) data(response).get("id");
    }

    private String uploadPhoto(String token, String activityId, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("category", "PHOTO");
        body.add("file", new NamedByteArrayResource(new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00}, filename));
        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/activities/" + activityId + "/files",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) data(response).get("id");
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

    private Map<String, Object> activityPayload(String title) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", "Activity description");
        payload.put("startTime", "2026-11-01T09:00:00");
        payload.put("endTime", "2026-11-01T12:00:00");
        payload.put("location", "Science Hall");
        payload.put("capacity", 50);
        payload.put("registrationDeadline", "2026-10-31T18:00:00");
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

    private Map<String, Object> questionPayload(String title, String type, boolean required, int sortOrder) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("type", type);
        payload.put("required", required);
        payload.put("sortOrder", sortOrder);
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
        if (token != null) {
            headers.setBearerAuth(token);
        }
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

    private Map<?, ?> data(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (Map<?, ?>) response.getBody().get("data");
    }

    private List<?> dataList(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (List<?>) response.getBody().get("data");
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
