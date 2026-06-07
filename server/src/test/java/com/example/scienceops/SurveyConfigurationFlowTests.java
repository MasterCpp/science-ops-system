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
class SurveyConfigurationFlowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void activityCanHaveOnlyOneSurveyAndSurveyTitleDescriptionCanBeUpdated() {
        String token = login("activityadmin");
        String activityId = createActivity(token, "Survey Activity");

        ResponseEntity<Map> created = exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/survey", token, surveyPayload("Initial Survey", "Initial description"));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        String surveyId = (String) data(created).get("id");
        assertThat(data(created).get("status")).isEqualTo("DRAFT");

        ResponseEntity<Map> duplicate = exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/survey", token, surveyPayload("Duplicate Survey", "Duplicate"));
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("DUPLICATE_SUBMISSION");

        ResponseEntity<Map> updated = exchange(HttpMethod.PUT, "/api/admin/surveys/" + surveyId, token, surveyPayload("Updated Survey", "Updated description"));
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(updated).get("title")).isEqualTo("Updated Survey");
        assertThat(data(updated).get("description")).isEqualTo("Updated description");
    }

    @Test
    void surveySupportsQuestionTypesOptionsSortingPublishAndClose() {
        String token = login("activityadmin");
        String activityId = createActivity(token, "Survey Editor Activity");
        String surveyId = (String) data(exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/survey", token, surveyPayload("Satisfaction Survey", "Tell us"))).get("id");

        Map<?, ?> rating = data(exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/questions", token, questionPayload("Rate the activity", "RATING", true, 30)));
        Map<?, ?> single = data(exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/questions", token, questionPayload("Favorite part", "SINGLE_CHOICE", true, 10)));
        Map<?, ?> multiple = data(exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/questions", token, questionPayload("Useful topics", "MULTIPLE_CHOICE", false, 20)));
        Map<?, ?> text = data(exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/questions", token, questionPayload("Suggestion", "TEXT", false, 40)));

        String singleId = (String) single.get("id");
        String multipleId = (String) multiple.get("id");
        String ratingId = (String) rating.get("id");

        Map<?, ?> laterOption = data(exchange(HttpMethod.POST, "/api/admin/survey-questions/" + singleId + "/options", token, optionPayload("Lecture", "lecture", 20)));
        Map<?, ?> earlierOption = data(exchange(HttpMethod.POST, "/api/admin/survey-questions/" + singleId + "/options", token, optionPayload("Experiment", "experiment", 10)));
        exchange(HttpMethod.POST, "/api/admin/survey-questions/" + multipleId + "/options", token, optionPayload("Astronomy", "astronomy", 10));
        exchange(HttpMethod.POST, "/api/admin/survey-questions/" + multipleId + "/options", token, optionPayload("Physics", "physics", 20));

        ResponseEntity<Map> invalidOption = exchange(HttpMethod.POST, "/api/admin/survey-questions/" + ratingId + "/options", token, optionPayload("Five", "5", 1));
        assertThat(invalidOption.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalidOption.getBody().get("code")).isEqualTo("INVALID_REQUEST");

        ResponseEntity<Map> optionUpdate = exchange(HttpMethod.PUT, "/api/admin/survey-options/" + laterOption.get("id"), token, optionPayload("Lecture Updated", "lecture-updated", 30));
        assertThat(optionUpdate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(optionUpdate).get("label")).isEqualTo("Lecture Updated");

        ResponseEntity<Map> questionUpdate = exchange(HttpMethod.PUT, "/api/admin/survey-questions/" + ratingId, token, questionPayload("Rate the activity overall", "RATING", true, 15));
        assertThat(questionUpdate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(questionUpdate).get("sortOrder")).isEqualTo(15);

        ResponseEntity<Map> detail = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/survey", token, null);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> questions = (List<?>) data(detail).get("questions");
        assertThat(questions).hasSize(4);
        assertThat(((Map<?, ?>) questions.get(0)).get("type")).isEqualTo("SINGLE_CHOICE");
        assertThat(((Map<?, ?>) questions.get(1)).get("type")).isEqualTo("RATING");
        assertThat(((Map<?, ?>) questions.get(2)).get("type")).isEqualTo("MULTIPLE_CHOICE");
        assertThat(((Map<?, ?>) questions.get(3)).get("type")).isEqualTo("TEXT");

        List<?> singleOptions = (List<?>) ((Map<?, ?>) questions.get(0)).get("options");
        assertThat(((Map<?, ?>) singleOptions.get(0)).get("id")).isEqualTo(earlierOption.get("id"));
        assertThat(((Map<?, ?>) singleOptions.get(1)).get("label")).isEqualTo("Lecture Updated");

        ResponseEntity<Map> published = exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/publish", token, null);
        assertThat(published.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(published).get("status")).isEqualTo("PUBLISHED");

        ResponseEntity<Map> closed = exchange(HttpMethod.POST, "/api/admin/surveys/" + surveyId + "/close", token, null);
        assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(closed).get("status")).isEqualTo("CLOSED");

        ResponseEntity<Map> deletedOption = exchange(HttpMethod.DELETE, "/api/admin/survey-options/" + earlierOption.get("id"), token, null);
        assertThat(deletedOption.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> deletedQuestion = exchange(HttpMethod.DELETE, "/api/admin/survey-questions/" + text.get("id"), token, null);
        assertThat(deletedQuestion.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void volunteerAdminCannotManageSurveys() {
        String activityToken = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createActivity(activityToken, "Survey Permission Activity");

        ResponseEntity<Map> denied = exchange(HttpMethod.POST, "/api/admin/activities/" + activityId + "/survey", volunteerToken, surveyPayload("Denied Survey", "Denied"));
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String createActivity(String token, String title) {
        return (String) data(exchange(HttpMethod.POST, "/api/admin/activities", token, activityPayload(title))).get("id");
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

    private Map<?, ?> data(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (Map<?, ?>) response.getBody().get("data");
    }
}
