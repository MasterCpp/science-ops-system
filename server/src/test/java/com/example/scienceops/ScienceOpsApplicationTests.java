package com.example.scienceops;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScienceOpsApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointReturnsBaselineEnvelope() {
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/api/health", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"success\":true");
        assertThat(response.getBody()).contains("\"service\":\"science-ops-server\"");
        assertThat(response.getBody()).contains("\"message\":\"OK\"");
    }

    @Test
    void adminLoginSucceedsAndMeReturnsRolesAndPermissions() {
        String token = login("superadmin", "password123");

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/auth/me",
                HttpMethod.GET,
                authorized(token),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"username\":\"superadmin\"");
        assertThat(response.getBody()).contains("\"roles\":[\"SUPER_ADMIN\"]");
        assertThat(response.getBody()).contains("admin-user:manage");
        assertThat(response.getBody()).contains("operation-log:view");
    }

    @Test
    void adminLoginRejectsInvalidPasswordAndDisabledAccount() throws Exception {
        HttpResponse<String> invalidPassword = postLoginWithHttpClient("superadmin", "wrong-password");
        assertThat(invalidPassword.statusCode()).isEqualTo(401);
        assertThat(invalidPassword.body()).contains("\"code\":\"UNAUTHORIZED\"");

        HttpResponse<String> disabled = postLoginWithHttpClient("disabledadmin", "password123");
        assertThat(disabled.statusCode()).isEqualTo(403);
        assertThat(disabled.body()).contains("\"code\":\"FORBIDDEN\"");
    }

    @Test
    void adminProtectedEndpointsRejectMissingAndInvalidJwt() {
        ResponseEntity<String> missing = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/admin/auth/me",
                String.class
        );
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(missing.getBody()).contains("\"code\":\"UNAUTHORIZED\"");

        ResponseEntity<String> invalid = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/auth/me",
                HttpMethod.GET,
                authorized("invalid.jwt.token"),
                String.class
        );
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(invalid.getBody()).contains("\"code\":\"UNAUTHORIZED\"");
    }

    @Test
    void superAdminCanAccessAccountAndOperationLogPermissions() {
        String token = login("superadmin", "password123");

        assertThat(getWithToken("/api/admin/users", token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getWithToken("/api/admin/operation-logs", token).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void activityAdminCannotAccessAccountOrOperationLogs() {
        String token = login("activityadmin", "password123");

        assertThat(getWithToken("/api/admin/rbac/probes/registration", token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getWithToken("/api/admin/users", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken("/api/admin/operation-logs", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void volunteerAdminCannotAccessRegistrationSurveyVisitorFileOrAccountPermissions() {
        String token = login("volunteeradmin", "password123");

        assertThat(getWithToken("/api/admin/rbac/probes/registration", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken("/api/admin/rbac/probes/survey", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken("/api/admin/rbac/probes/visitor-report", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken("/api/admin/rbac/probes/file", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken("/api/admin/users", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void flywayCreatesCoreTablesAndCommonColumns() {
        String[] businessTables = {
                "admin_user",
                "role",
                "permission",
                "admin_user_role",
                "role_permission",
                "activity",
                "activity_process_item",
                "activity_custom_field",
                "registration",
                "registration_custom_value",
                "check_in",
                "volunteer_position",
                "volunteer_application",
                "volunteer_attendance",
                "visitor_report",
                "survey",
                "survey_question",
                "survey_option",
                "survey_response",
                "survey_answer",
                "file_asset"
        };

        for (String tableName : businessTables) {
            assertThat(tableExists(tableName)).as(tableName + " exists").isTrue();
            assertThat(columnExists(tableName, "id")).as(tableName + ".id exists").isTrue();
            assertThat(columnExists(tableName, "created_at")).as(tableName + ".created_at exists").isTrue();
            assertThat(columnExists(tableName, "updated_at")).as(tableName + ".updated_at exists").isTrue();
            assertThat(columnExists(tableName, "deleted")).as(tableName + ".deleted exists").isTrue();
        }

        assertThat(tableExists("operation_log")).isTrue();
        assertThat(columnExists("operation_log", "created_at")).isTrue();
        assertThat(columnExists("operation_log", "deleted")).isFalse();
    }

    @Test
    void auditedTablesContainCreatedByAndUpdatedBy() {
        String[] auditedTables = {
                "activity",
                "activity_process_item",
                "activity_custom_field",
                "volunteer_position",
                "visitor_report",
                "survey"
        };

        for (String tableName : auditedTables) {
            assertThat(columnExists(tableName, "created_by")).as(tableName + ".created_by exists").isTrue();
            assertThat(columnExists(tableName, "updated_by")).as(tableName + ".updated_by exists").isTrue();
        }
    }

    @Test
    void migrationCreatesConfirmedUniqueConstraintsAndIndexes() {
        String[] expectedUniqueIndexes = {
                "ux_admin_user_username",
                "ux_role_code",
                "ux_permission_code",
                "ux_admin_user_role_user_role",
                "ux_role_permission_role_permission",
                "ux_activity_custom_field_activity_key",
                "ux_registration_activity_phone",
                "ux_check_in_registration_id",
                "ux_volunteer_application_activity_phone",
                "ux_volunteer_attendance_application_id",
                "ux_survey_activity_id",
                "ux_survey_response_survey_registration",
                "ux_survey_answer_response_question"
        };

        for (String indexName : expectedUniqueIndexes) {
            assertThat(constraintExists(indexName)).as(indexName + " exists").isTrue();
        }

        String[] expectedNormalIndexes = {
                "idx_activity_status",
                "idx_activity_start_time",
                "idx_registration_activity_id",
                "idx_registration_phone",
                "idx_registration_status",
                "idx_check_in_activity_id",
                "idx_volunteer_position_activity_id",
                "idx_volunteer_application_position_id",
                "idx_volunteer_application_status",
                "idx_volunteer_attendance_activity_id",
                "idx_visitor_report_activity_id",
                "idx_survey_question_survey_id",
                "idx_survey_answer_response_id",
                "idx_file_asset_activity_category",
                "idx_operation_log_admin_user_id",
                "idx_operation_log_action",
                "idx_operation_log_target",
                "idx_operation_log_created_at"
        };

        for (String indexName : expectedNormalIndexes) {
            assertThat(indexExists(indexName)).as(indexName + " exists").isTrue();
        }
    }


    @Test
    void flywayMigrationCanRunOnFreshDatabasesRepeatedly() {
        migrateFreshDatabase("science_ops_repeat_one");
        migrateFreshDatabase("science_ops_repeat_two");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where lower(table_name) = ? and lower(column_name) = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private void migrateFreshDatabase(String databaseName) {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
                        "sa",
                        ""
                )
                .locations("classpath:db/migration")
                .load();

        assertThat(flyway.migrate().success).isTrue();
    }

    private boolean indexExists(String expectedIndexName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.indexes where lower(index_name) = ?",
                Integer.class,
                expectedIndexName
        );
        return count != null && count > 0;
    }

    private boolean constraintExists(String expectedConstraintName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.table_constraints where lower(constraint_name) = ?",
                Integer.class,
                expectedConstraintName
        );
        return count != null && count > 0;
    }

    private String login(String username, String password) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/admin/auth/login",
                loginRequest(username, password),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        return (String) data.get("token");
    }

    private HttpEntity<Map<String, String>> loginRequest(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of("username", username, "password", password), headers);
    }

    private ResponseEntity<String> getWithToken(String path, String token) {
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                HttpMethod.GET,
                authorized(token),
                String.class
        );
    }

    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private HttpResponse<String> postLoginWithHttpClient(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/admin/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
