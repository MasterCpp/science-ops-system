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
class VisitorReportFlowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void activityAdminCanCreateVisitorReportsWithAndWithoutLinkedActivity() {
        String token = login("activityadmin");
        String activityId = createActivity(token, "Visitor Linked Activity");

        ResponseEntity<Map> linked = exchange(
                HttpMethod.POST,
                "/api/admin/visitor-reports",
                token,
                visitorReportPayload(activityId, "Science Museum", "Mina", "13810000001", "2026-11-02T09:30:00")
        );
        assertThat(linked.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(linked).get("activityId")).isEqualTo(activityId);
        assertThat(data(linked).get("activityTitle")).isEqualTo("Visitor Linked Activity");

        ResponseEntity<Map> unlinked = exchange(
                HttpMethod.POST,
                "/api/admin/visitor-reports",
                token,
                visitorReportPayload(null, "Community Group", "Noah", "13810000002", "2026-11-03T10:00:00")
        );
        assertThat(unlinked.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(unlinked).get("activityId")).isNull();
        assertThat(data(unlinked).get("visitorUnit")).isEqualTo("Community Group");
    }

    @Test
    void activityAdminCanListFilterUpdateDeleteAndExportVisitorReports() {
        String token = login("activityadmin");
        String activityId = createActivity(token, "Visitor Filter Activity");
        Map<?, ?> created = data(exchange(
                HttpMethod.POST,
                "/api/admin/visitor-reports",
                token,
                visitorReportPayload(activityId, "Planet Lab", "Olivia", "13810000003", "2026-11-04T14:00:00")
        ));
        String reportId = (String) created.get("id");
        exchange(
                HttpMethod.POST,
                "/api/admin/visitor-reports",
                token,
                visitorReportPayload(null, "Archive Team", "Pete", "13810000004", "2026-11-05T14:00:00")
        );

        ResponseEntity<Map> keywordList = exchange(HttpMethod.GET, "/api/admin/visitor-reports?keyword=Planet", token, null);
        assertThat(keywordList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(keywordList).get("total")).isEqualTo(1);

        ResponseEntity<Map> activityList = exchange(HttpMethod.GET, "/api/admin/visitor-reports?activityId=" + activityId, token, null);
        assertThat(activityList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(activityList).get("total")).isEqualTo(1);

        ResponseEntity<Map> dateList = exchange(HttpMethod.GET, "/api/admin/visitor-reports?visitFrom=2026-11-04&visitTo=2026-11-04", token, null);
        assertThat(dateList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(dateList).get("total")).isEqualTo(1);

        ResponseEntity<Map> updated = exchange(
                HttpMethod.PUT,
                "/api/admin/visitor-reports/" + reportId,
                token,
                visitorReportPayload(null, "Updated Planet Lab", "Olivia", "13810000003", "2026-11-06T15:00:00")
        );
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(updated).get("activityId")).isNull();
        assertThat(data(updated).get("visitorUnit")).isEqualTo("Updated Planet Lab");

        ResponseEntity<String> export = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/visitor-reports/export?keyword=Updated",
                HttpMethod.GET,
                authorized(token),
                String.class
        );
        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(export.getBody()).contains("Visitor Unit,Contact Name,Contact Phone,Visitor Count,Visit Date,Visit Reason,Activity,Remark");
        assertThat(export.getBody()).contains("Updated Planet Lab");

        ResponseEntity<Map> deleted = exchange(HttpMethod.DELETE, "/api/admin/visitor-reports/" + reportId, token, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> afterDelete = exchange(HttpMethod.GET, "/api/admin/visitor-reports?keyword=Updated", token, null);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(afterDelete).get("total")).isEqualTo(0);
    }

    @Test
    void volunteerAdminCannotManageVisitorReports() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createActivity(activityToken, "Visitor Permission Activity");

        ResponseEntity<Map> deniedCreate = exchange(
                HttpMethod.POST,
                "/api/admin/visitor-reports",
                volunteerToken,
                visitorReportPayload(activityId, "Volunteer Denied Unit", "Quinn", "13810000005", "2026-11-07T09:00:00")
        );
        assertThat(deniedCreate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> deniedList = exchange(HttpMethod.GET, "/api/admin/visitor-reports", volunteerToken, null);
        assertThat(deniedList.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> deniedExport = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/visitor-reports/export",
                HttpMethod.GET,
                authorized(volunteerToken),
                String.class
        );
        assertThat(deniedExport.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String createActivity(String token, String title) {
        return (String) data(exchange(HttpMethod.POST, "/api/admin/activities", token, activityPayload(title))).get("id");
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

    private Map<String, Object> visitorReportPayload(
            String activityId,
            String visitorUnit,
            String contactName,
            String contactPhone,
            String visitDate
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activityId", activityId);
        payload.put("visitorUnit", visitorUnit);
        payload.put("contactName", contactName);
        payload.put("contactPhone", contactPhone);
        payload.put("visitorCount", 6);
        payload.put("visitDate", visitDate);
        payload.put("visitReason", "Science exchange");
        payload.put("remark", "Bring badges");
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
