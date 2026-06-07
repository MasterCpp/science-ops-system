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
class SurveyResponseFlowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void surveyEligibilityRejectsUnregisteredUncheckedAndUnpublishedStates() {
        String token = login("activityadmin");
        String activityId = createPublishedActivity(token, "Survey Eligibility Activity");
        Map<?, ?> registration = data(publicRegister(activityId, registrationPayload("Alice", "13900000001")));
        String registrationId = (String) registration.get("id");
        publicRegister(activityId, registrationPayload("Alan", "13900000011"));
        String surveyId = createSurveyWithQuestions(token, activityId).surveyId();

        ResponseEntity<Map> unregistered = exchange(HttpMethod.GET, "/api/mobile/activities/" + activityId + "/survey/eligibility?phone=13999999999", null, null);
        assertThat(unregistered.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        adminCheckIn(token, activityId, registrationId);
        ResponseEntity<Map> notPublished = exchange(HttpMethod.GET, "/api/mobile/activities/" + activityId + "/survey/eligibility?phone=13900000001", null, null);
        assertThat(notPublished.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(notPublished.getBody().get("code")).isEqualTo("INVALID_STATE");

        exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/publish", token, null);

        ResponseEntity<Map> notCheckedIn = exchange(HttpMethod.GET, "/api/mobile/activities/" + activityId + "/survey/eligibility?phone=13900000011", null, null);
        assertThat(notCheckedIn.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(notCheckedIn.getBody().get("code")).isEqualTo("NOT_CHECKED_IN");

        ResponseEntity<Map> eligible = exchange(HttpMethod.GET, "/api/mobile/activities/" + activityId + "/survey/eligibility?phone=13900000001", null, null);
        assertThat(eligible.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(eligible).get("eligible")).isEqualTo(true);
    }

    @Test
    void checkedInAudienceCanSubmitOnceAndAdminCanViewStatisticsResponsesAndExport() {
        String token = login("activityadmin");
        String activityId = createPublishedActivity(token, "Survey Response Activity");
        Map<?, ?> registration = data(publicRegister(activityId, registrationPayload("Betty", "13900000002")));
        String registrationId = (String) registration.get("id");
        SurveyFixture survey = createSurveyWithQuestions(token, activityId);
        exchange(HttpMethod.POST, "/api/admin/surveys/" + survey.surveyId() + "/publish", token, null);
        adminCheckIn(token, activityId, registrationId);

        ResponseEntity<Map> detail = exchange(HttpMethod.GET, "/api/mobile/activities/" + activityId + "/survey?phone=13900000002", null, null);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<?>) data(detail).get("questions"))).hasSize(4);

        ResponseEntity<Map> submitted = exchange(
                HttpMethod.POST,
                "/api/mobile/activities/" + activityId + "/survey/responses",
                null,
                submitPayload("13900000002", survey)
        );
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(submitted).get("respondentName")).isEqualTo("Betty");

        ResponseEntity<Map> duplicate = exchange(
                HttpMethod.POST,
                "/api/mobile/activities/" + activityId + "/survey/responses",
                null,
                submitPayload("13900000002", survey)
        );
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("DUPLICATE_SUBMISSION");

        ResponseEntity<Map> lockedEdit = exchange(HttpMethod.PUT, "/api/admin/surveys/" + survey.surveyId(), token, surveyPayload("Edited", "Should fail"));
        assertThat(lockedEdit.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(lockedEdit.getBody().get("code")).isEqualTo("INVALID_STATE");

        ResponseEntity<Map> statistics = exchange(HttpMethod.GET, "/api/admin/surveys/" + survey.surveyId() + "/statistics", token, null);
        assertThat(statistics.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(statistics).get("responseCount")).isEqualTo(1);
        assertThat(String.valueOf(data(statistics).get("averageRating"))).startsWith("5");
        assertThat(((List<?>) data(statistics).get("questions"))).hasSize(4);

        ResponseEntity<Map> responses = exchange(HttpMethod.GET, "/api/admin/surveys/" + survey.surveyId() + "/responses", token, null);
        assertThat(responses.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(responses).get("total")).isEqualTo(1);
        String responseText = responses.getBody().toString();
        assertThat(responseText).contains("Betty").contains("Great activity");

        ResponseEntity<String> export = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/surveys/" + survey.surveyId() + "/export",
                HttpMethod.GET,
                authorized(token),
                String.class
        );
        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(export.getBody()).contains("Response ID,Survey,Registration ID,Name,Phone,Submitted At,Answers");
        assertThat(export.getBody()).contains("Betty").contains("Great activity");

        ResponseEntity<Map> closed = exchange(HttpMethod.POST, "/api/admin/surveys/" + survey.surveyId() + "/close", token, null);
        assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(closed).get("status")).isEqualTo("CLOSED");
    }

    @Test
    void closedSurveyCannotAcceptSubmissions() {
        String token = login("activityadmin");
        String activityId = createPublishedActivity(token, "Closed Survey Activity");
        Map<?, ?> registration = data(publicRegister(activityId, registrationPayload("Cindy", "13900000003")));
        String registrationId = (String) registration.get("id");
        SurveyFixture survey = createSurveyWithQuestions(token, activityId);
        exchange(HttpMethod.POST, "/api/admin/surveys/" + survey.surveyId() + "/publish", token, null);
        exchange(HttpMethod.POST, "/api/admin/surveys/" + survey.surveyId() + "/close", token, null);
        adminCheckIn(token, activityId, registrationId);

        ResponseEntity<Map> closedSubmit = exchange(
                HttpMethod.POST,
                "/api/mobile/activities/" + activityId + "/survey/responses",
                null,
                submitPayload("13900000003", survey)
        );
        assertThat(closedSubmit.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(closedSubmit.getBody().get("code")).isEqualTo("INVALID_STATE");
    }

    private SurveyFixture createSurveyWithQuestions(String token, String activityId) {
        String surveyId = (String) data(exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/survey", token, surveyPayload("Satisfaction Survey", "Tell us"))).get("id");
        String singleId = (String) data(exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/questions", token, questionPayload("Favorite part", "SINGLE_CHOICE", true, 10))).get("id");
        String singleOptionId = (String) data(exchange(HttpMethod.POST, "/api/admin/survey-questions/" + singleId + "/options", token, optionPayload("Experiment", "experiment", 10))).get("id");
        String multipleId = (String) data(exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/questions", token, questionPayload("Useful topics", "MULTIPLE_CHOICE", true, 20))).get("id");
        String multipleOptionId = (String) data(exchange(HttpMethod.POST, "/api/admin/survey-questions/" + multipleId + "/options", token, optionPayload("Astronomy", "astronomy", 10))).get("id");
        String ratingId = (String) data(exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/questions", token, questionPayload("Rating", "RATING", true, 30))).get("id");
        String textId = (String) data(exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/questions", token, questionPayload("Suggestion", "TEXT", false, 40))).get("id");
        return new SurveyFixture(surveyId, singleId, singleOptionId, multipleId, multipleOptionId, ratingId, textId);
    }

    private void adminCheckIn(String token, String activityId, String registrationId) {
        ResponseEntity<Map> response = exchange(
                HttpMethod.POST,
                "/api/admin/activities/" + activityId + "/check-ins/manual",
                token,
                Map.of("registrationId", registrationId, "checkInTime", "2026-11-01T09:30:00")
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<Map> publicRegister(String activityId, Map<String, Object> payload) {
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/api/mobile/activities/" + activityId + "/registrations",
                json(payload),
                Map.class
        );
    }

    private String createPublishedActivity(String token, String title) {
        String id = (String) data(exchange(HttpMethod.POST, "/api/admin/activities", token, activityPayload(title))).get("id");
        exchange(HttpMethod.POST, "/api/admin/activities/" + id + "/publish", token, null);
        return id;
    }

    private Map<String, Object> submitPayload(String phone, SurveyFixture survey) {
        return Map.of(
                "phone", phone,
                "answers", List.of(
                        Map.of("questionId", survey.singleQuestionId(), "optionId", survey.singleOptionId()),
                        Map.of("questionId", survey.multipleQuestionId(), "optionIds", List.of(survey.multipleOptionId())),
                        Map.of("questionId", survey.ratingQuestionId(), "numericValue", 5),
                        Map.of("questionId", survey.textQuestionId(), "textValue", "Great activity")
                )
        );
    }

    private Map<String, Object> surveyPayload(String title, String description) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", description);
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

    private Map<String, Object> optionPayload(String label, String value, int sortOrder) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("label", label);
        payload.put("value", value);
        payload.put("sortOrder", sortOrder);
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

    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private Map<?, ?> data(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (Map<?, ?>) response.getBody().get("data");
    }

    private record SurveyFixture(
            String surveyId,
            String singleQuestionId,
            String singleOptionId,
            String multipleQuestionId,
            String multipleOptionId,
            String ratingQuestionId,
            String textQuestionId
    ) {
    }
}
