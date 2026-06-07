package com.example.scienceops.survey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class SurveyRepository {

    private final JdbcTemplate jdbcTemplate;

    SurveyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    boolean activityExists(Long activityId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from activity where id = ? and deleted = 0",
                Long.class,
                activityId
        );
        return count != null && count > 0;
    }

    boolean surveyExistsForActivity(Long activityId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from survey where activity_id = ? and deleted = 0",
                Long.class,
                activityId
        );
        return count != null && count > 0;
    }

    void insertSurvey(Long id, Long activityId, SurveyRequest request, String status, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into survey
                          (id, activity_id, title, description, status, created_by, updated_by, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                activityId,
                request.title(),
                request.description(),
                status,
                adminUserId,
                adminUserId,
                now,
                now
        );
    }

    void updateSurvey(Long surveyId, SurveyRequest request, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update survey
                        set title = ?, description = ?, updated_by = ?, updated_at = ?
                        where id = ? and deleted = 0
                        """,
                request.title(),
                request.description(),
                adminUserId,
                now,
                surveyId
        );
    }

    void updateSurveyStatus(Long surveyId, String status, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                "update survey set status = ?, updated_by = ?, updated_at = ? where id = ? and deleted = 0",
                status,
                adminUserId,
                now,
                surveyId
        );
    }

    Optional<SurveyRecord> findSurveyByActivityId(Long activityId) {
        return jdbcTemplate.query(
                selectSurveySql() + " and activity_id = ?",
                this::mapSurvey,
                activityId
        ).stream().findFirst();
    }

    Optional<SurveyRecord> findSurvey(Long surveyId) {
        return jdbcTemplate.query(
                selectSurveySql() + " and id = ?",
                this::mapSurvey,
                surveyId
        ).stream().findFirst();
    }

    void insertQuestion(Long id, Long surveyId, SurveyQuestionRequest request, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into survey_question
                          (id, survey_id, title, type, required, sort_order, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                surveyId,
                request.title(),
                request.type(),
                Boolean.TRUE.equals(request.required()) ? 1 : 0,
                safeSortOrder(request.sortOrder()),
                now,
                now
        );
    }

    void updateQuestion(Long questionId, SurveyQuestionRequest request, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update survey_question
                        set title = ?, type = ?, required = ?, sort_order = ?, updated_at = ?
                        where id = ? and deleted = 0
                        """,
                request.title(),
                request.type(),
                Boolean.TRUE.equals(request.required()) ? 1 : 0,
                safeSortOrder(request.sortOrder()),
                now,
                questionId
        );
    }

    void deleteQuestion(Long questionId, LocalDateTime now) {
        jdbcTemplate.update(
                "update survey_question set deleted = 1, updated_at = ? where id = ? and deleted = 0",
                now,
                questionId
        );
        jdbcTemplate.update(
                "update survey_option set deleted = 1, updated_at = ? where question_id = ? and deleted = 0",
                now,
                questionId
        );
    }

    Optional<SurveyQuestionRecord> findQuestion(Long questionId) {
        return jdbcTemplate.query(
                selectQuestionSql() + " where id = ? and deleted = 0",
                this::mapQuestion,
                questionId
        ).stream().findFirst();
    }

    List<SurveyQuestionRecord> listQuestions(Long surveyId) {
        return jdbcTemplate.query(
                selectQuestionSql() + " where survey_id = ? and deleted = 0 order by sort_order asc, id asc",
                this::mapQuestion,
                surveyId
        );
    }

    void insertOption(Long id, Long questionId, SurveyOptionRequest request, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into survey_option
                          (id, question_id, label, `value`, sort_order, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                questionId,
                request.label(),
                request.value(),
                safeSortOrder(request.sortOrder()),
                now,
                now
        );
    }

    void updateOption(Long optionId, SurveyOptionRequest request, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update survey_option
                        set label = ?, `value` = ?, sort_order = ?, updated_at = ?
                        where id = ? and deleted = 0
                        """,
                request.label(),
                request.value(),
                safeSortOrder(request.sortOrder()),
                now,
                optionId
        );
    }

    void deleteOption(Long optionId, LocalDateTime now) {
        jdbcTemplate.update(
                "update survey_option set deleted = 1, updated_at = ? where id = ? and deleted = 0",
                now,
                optionId
        );
    }

    Optional<SurveyOptionRecord> findOption(Long optionId) {
        return jdbcTemplate.query(
                selectOptionSql() + " where id = ? and deleted = 0",
                this::mapOption,
                optionId
        ).stream().findFirst();
    }

    List<SurveyOptionRecord> listOptions(Long questionId) {
        return jdbcTemplate.query(
                selectOptionSql() + " where question_id = ? and deleted = 0 order by sort_order asc, id asc",
                this::mapOption,
                questionId
        );
    }

    Optional<SurveyRegistrationRecord> findRegistrationForSurvey(Long activityId, String phone) {
        return jdbcTemplate.query(
                """
                        select r.id,
                               r.activity_id,
                               r.name,
                               r.phone,
                               r.status,
                               c.status as check_in_status
                        from registration r
                        left join check_in c on c.registration_id = r.id and c.deleted = 0
                        where r.activity_id = ? and r.phone = ? and r.deleted = 0
                        """,
                this::mapRegistration,
                activityId,
                phone
        ).stream().findFirst();
    }

    boolean responseExists(Long surveyId, Long registrationId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from survey_response where survey_id = ? and registration_id = ? and deleted = 0",
                Long.class,
                surveyId,
                registrationId
        );
        return count != null && count > 0;
    }

    boolean hasResponses(Long surveyId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from survey_response where survey_id = ? and deleted = 0",
                Long.class,
                surveyId
        );
        return count != null && count > 0;
    }

    void insertResponse(Long id, Long surveyId, Long registrationId, String respondentName, String respondentPhone, LocalDateTime submittedAt) {
        jdbcTemplate.update(
                """
                        insert into survey_response
                          (id, survey_id, registration_id, respondent_name, respondent_phone, submitted_at, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                surveyId,
                registrationId,
                respondentName,
                respondentPhone,
                submittedAt,
                submittedAt,
                submittedAt
        );
    }

    void insertAnswer(Long id, Long responseId, Long questionId, Long optionId, String optionIdsJson,
                      BigDecimal numericValue, String textValue, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into survey_answer
                          (id, response_id, question_id, option_id, option_ids_json, numeric_value, text_value, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                responseId,
                questionId,
                optionId,
                optionIdsJson,
                numericValue,
                textValue,
                now,
                now
        );
    }

    Optional<SurveyResponseRecord> findResponse(Long responseId) {
        return jdbcTemplate.query(
                selectResponseSql() + " where id = ? and deleted = 0",
                this::mapResponse,
                responseId
        ).stream().findFirst();
    }

    List<SurveyResponseRecord> listResponses(Long surveyId, int page, int pageSize) {
        return jdbcTemplate.query(
                selectResponseSql() + " where survey_id = ? and deleted = 0 order by submitted_at desc, id desc limit ? offset ?",
                this::mapResponse,
                surveyId,
                pageSize,
                (page - 1) * pageSize
        );
    }

    long countResponses(Long surveyId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from survey_response where survey_id = ? and deleted = 0",
                Long.class,
                surveyId
        );
        return count == null ? 0 : count;
    }

    BigDecimal averageRating(Long surveyId) {
        return jdbcTemplate.queryForObject(
                """
                        select avg(sa.numeric_value)
                        from survey_answer sa
                        join survey_response sr on sr.id = sa.response_id and sr.deleted = 0
                        join survey_question sq on sq.id = sa.question_id and sq.deleted = 0
                        where sr.survey_id = ? and sa.deleted = 0 and sq.type = 'RATING' and sa.numeric_value is not null
                        """,
                BigDecimal.class,
                surveyId
        );
    }

    long countAnswers(Long questionId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from survey_answer sa
                        join survey_response sr on sr.id = sa.response_id and sr.deleted = 0
                        where sa.question_id = ? and sa.deleted = 0
                        """,
                Long.class,
                questionId
        );
        return count == null ? 0 : count;
    }

    BigDecimal averageRatingForQuestion(Long questionId) {
        return jdbcTemplate.queryForObject(
                """
                        select avg(numeric_value)
                        from survey_answer
                        where question_id = ? and deleted = 0 and numeric_value is not null
                        """,
                BigDecimal.class,
                questionId
        );
    }

    long countOptionSelected(Long optionId) {
        String quoted = "\"" + optionId + "\"";
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from survey_answer
                        where deleted = 0
                          and (option_id = ? or option_ids_json like ?)
                        """,
                Long.class,
                optionId,
                "%" + quoted + "%"
        );
        return count == null ? 0 : count;
    }

    List<SurveyAnswerRecord> listAnswersForResponse(Long responseId) {
        return jdbcTemplate.query(
                """
                        select sa.response_id,
                               sa.question_id,
                               sq.title as question_title,
                               sq.type as question_type,
                               sa.option_id,
                               so.label as option_label,
                               sa.option_ids_json,
                               sa.numeric_value,
                               sa.text_value
                        from survey_answer sa
                        join survey_question sq on sq.id = sa.question_id and sq.deleted = 0
                        left join survey_option so on so.id = sa.option_id and so.deleted = 0
                        where sa.response_id = ? and sa.deleted = 0
                        order by sq.sort_order asc, sq.id asc
                        """,
                this::mapAnswer,
                responseId
        );
    }

    private String selectSurveySql() {
        return """
                select id, activity_id, title, description, status, created_by, updated_by, created_at, updated_at
                from survey
                where deleted = 0
                """;
    }

    private String selectQuestionSql() {
        return """
                select id, survey_id, title, type, required, sort_order, created_at, updated_at
                from survey_question
                """;
    }

    private String selectOptionSql() {
        return """
                select id, question_id, label, `value`, sort_order, created_at, updated_at
                from survey_option
                """;
    }

    private String selectResponseSql() {
        return """
                select id, survey_id, registration_id, respondent_name, respondent_phone, submitted_at, created_at, updated_at
                from survey_response
                """;
    }

    private SurveyRecord mapSurvey(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SurveyRecord(
                resultSet.getLong("id"),
                resultSet.getLong("activity_id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("status"),
                getNullableLong(resultSet, "created_by"),
                getNullableLong(resultSet, "updated_by"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private SurveyQuestionRecord mapQuestion(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SurveyQuestionRecord(
                resultSet.getLong("id"),
                resultSet.getLong("survey_id"),
                resultSet.getString("title"),
                resultSet.getString("type"),
                resultSet.getInt("required") == 1,
                resultSet.getInt("sort_order"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private SurveyOptionRecord mapOption(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SurveyOptionRecord(
                resultSet.getLong("id"),
                resultSet.getLong("question_id"),
                resultSet.getString("label"),
                resultSet.getString("value"),
                resultSet.getInt("sort_order"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private SurveyRegistrationRecord mapRegistration(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SurveyRegistrationRecord(
                resultSet.getLong("id"),
                resultSet.getLong("activity_id"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getString("status"),
                resultSet.getString("check_in_status")
        );
    }

    private SurveyResponseRecord mapResponse(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SurveyResponseRecord(
                resultSet.getLong("id"),
                resultSet.getLong("survey_id"),
                resultSet.getLong("registration_id"),
                resultSet.getString("respondent_name"),
                resultSet.getString("respondent_phone"),
                resultSet.getObject("submitted_at", LocalDateTime.class),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private SurveyAnswerRecord mapAnswer(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SurveyAnswerRecord(
                resultSet.getLong("response_id"),
                resultSet.getLong("question_id"),
                resultSet.getString("question_title"),
                resultSet.getString("question_type"),
                getNullableLong(resultSet, "option_id"),
                resultSet.getString("option_label"),
                resultSet.getString("option_ids_json"),
                resultSet.getBigDecimal("numeric_value"),
                resultSet.getString("text_value")
        );
    }

    private Integer safeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    private Long getNullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
