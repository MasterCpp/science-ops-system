package com.example.scienceops.survey;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.common.enums.SurveyQuestionType;
import com.example.scienceops.common.enums.SurveyStatus;
import com.example.scienceops.common.error.BusinessRuleException;
import com.example.scienceops.common.error.NotFoundException;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.stereotype.Service;

@Service
public class SurveyService {

    private final SurveyRepository repository;
    private final Clock clock;

    public SurveyService(SurveyRepository repository) {
        this.repository = repository;
        this.clock = Clock.systemDefaultZone();
    }

    public SurveyResponse getByActivity(Long activityId) {
        requireActivity(activityId);
        return repository.findSurveyByActivityId(activityId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Survey not found"));
    }

    public SurveyResponse create(Long activityId, SurveyRequest request, AdminPrincipal principal) {
        requireActivity(activityId);
        if (repository.surveyExistsForActivity(activityId)) {
            throw new BusinessRuleException("DUPLICATE_SUBMISSION", "Activity already has a survey", 409);
        }
        Long id = IdWorker.getId();
        repository.insertSurvey(id, activityId, request, SurveyStatus.DRAFT.name(), principal.id(), now());
        return getSurvey(id);
    }

    public SurveyResponse update(Long surveyId, SurveyRequest request, AdminPrincipal principal) {
        SurveyRecord survey = requireSurvey(surveyId);
        ensureNoResponses(survey.id());
        repository.updateSurvey(surveyId, request, principal.id(), now());
        return getSurvey(surveyId);
    }

    public SurveyResponse publish(Long surveyId, AdminPrincipal principal) {
        requireSurvey(surveyId);
        repository.updateSurveyStatus(surveyId, SurveyStatus.PUBLISHED.name(), principal.id(), now());
        return getSurvey(surveyId);
    }

    public SurveyResponse close(Long surveyId, AdminPrincipal principal) {
        requireSurvey(surveyId);
        repository.updateSurveyStatus(surveyId, SurveyStatus.CLOSED.name(), principal.id(), now());
        return getSurvey(surveyId);
    }

    public SurveyQuestionResponse createQuestion(Long surveyId, SurveyQuestionRequest request) {
        SurveyRecord survey = requireSurvey(surveyId);
        ensureNoResponses(survey.id());
        SurveyQuestionType type = parseQuestionType(request.type());
        Long id = IdWorker.getId();
        repository.insertQuestion(id, surveyId, normalizedQuestionRequest(request, type), now());
        return getQuestion(id);
    }

    public SurveyQuestionResponse updateQuestion(Long questionId, SurveyQuestionRequest request) {
        SurveyQuestionRecord current = requireQuestion(questionId);
        ensureNoResponses(current.surveyId());
        SurveyQuestionType type = parseQuestionType(request.type());
        repository.updateQuestion(questionId, normalizedQuestionRequest(request, type), now());
        if (type != SurveyQuestionType.SINGLE_CHOICE && type != SurveyQuestionType.MULTIPLE_CHOICE) {
            for (SurveyOptionRecord option : repository.listOptions(questionId)) {
                repository.deleteOption(option.id(), now());
            }
        }
        return getQuestion(questionId);
    }

    public void deleteQuestion(Long questionId) {
        SurveyQuestionRecord question = requireQuestion(questionId);
        ensureNoResponses(question.surveyId());
        repository.deleteQuestion(questionId, now());
    }

    public SurveyOptionResponse createOption(Long questionId, SurveyOptionRequest request) {
        SurveyQuestionRecord question = requireQuestion(questionId);
        ensureNoResponses(question.surveyId());
        requireChoiceQuestion(question);
        Long id = IdWorker.getId();
        repository.insertOption(id, questionId, request, now());
        return getOption(id);
    }

    public SurveyOptionResponse updateOption(Long optionId, SurveyOptionRequest request) {
        SurveyOptionRecord option = requireOption(optionId);
        SurveyQuestionRecord question = requireQuestion(option.questionId());
        ensureNoResponses(question.surveyId());
        requireChoiceQuestion(question);
        repository.updateOption(optionId, request, now());
        return getOption(optionId);
    }

    public void deleteOption(Long optionId) {
        SurveyOptionRecord option = requireOption(optionId);
        SurveyQuestionRecord question = requireQuestion(option.questionId());
        ensureNoResponses(question.surveyId());
        repository.deleteOption(optionId, now());
    }

    public SurveyEligibilityResponse eligibility(Long activityId, String phone) {
        SurveyRecord survey = requireSurveyForActivity(activityId);
        SurveyRegistrationRecord registration = requireEligibleRegistration(activityId, phone);
        requirePublished(survey);
        if (repository.responseExists(survey.id(), registration.id())) {
            throw new BusinessRuleException("DUPLICATE_SUBMISSION", "Survey response already exists", 409);
        }
        return new SurveyEligibilityResponse(
                true,
                String.valueOf(survey.id()),
                String.valueOf(registration.id()),
                registration.name(),
                registration.phone(),
                "ELIGIBLE"
        );
    }

    public SurveyResponse publicSurvey(Long activityId, String phone) {
        eligibility(activityId, phone);
        return getByActivity(activityId);
    }

    public SurveySubmitResponse submit(Long activityId, SurveySubmitRequest request) {
        SurveyRecord survey = requireSurveyForActivity(activityId);
        SurveyRegistrationRecord registration = requireEligibleRegistration(activityId, request.phone());
        requirePublished(survey);
        if (repository.responseExists(survey.id(), registration.id())) {
            throw new BusinessRuleException("DUPLICATE_SUBMISSION", "Survey response already exists", 409);
        }

        List<SurveyQuestionRecord> questions = repository.listQuestions(survey.id());
        Map<Long, SurveyQuestionRecord> questionsById = questions.stream().collect(Collectors.toMap(SurveyQuestionRecord::id, question -> question));
        Map<Long, SurveyAnswerRequest> answersByQuestion = answersByQuestion(request.answers());
        requireRequiredAnswers(questions, answersByQuestion);

        LocalDateTime submittedAt = now();
        Long responseId = IdWorker.getId();
        repository.insertResponse(responseId, survey.id(), registration.id(), registration.name(), registration.phone(), submittedAt);
        for (SurveyAnswerRequest answer : request.answers()) {
            Long questionId = parseId(answer.questionId(), "Question id is invalid");
            SurveyQuestionRecord question = questionsById.get(questionId);
            if (question == null) {
                throw new NotFoundException("Survey question not found");
            }
            insertAnswer(responseId, question, answer, submittedAt);
        }
        return repository.findResponse(responseId)
                .map(this::toSubmitResponse)
                .orElseThrow(() -> new NotFoundException("Survey response not found"));
    }

    public SurveyStatisticsResponse statistics(Long surveyId) {
        SurveyRecord survey = requireSurvey(surveyId);
        List<SurveyQuestionStatisticsResponse> questionStats = repository.listQuestions(survey.id())
                .stream()
                .map(this::toQuestionStatistics)
                .toList();
        return new SurveyStatisticsResponse(
                String.valueOf(survey.id()),
                repository.countResponses(survey.id()),
                repository.averageRating(survey.id()),
                questionStats
        );
    }

    public PagedResponse<SurveyRawResponse> responses(Long surveyId, int page, int pageSize) {
        SurveyRecord survey = requireSurvey(surveyId);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return new PagedResponse<>(
                repository.listResponses(survey.id(), safePage, safePageSize).stream().map(this::toRawResponse).toList(),
                safePage,
                safePageSize,
                repository.countResponses(survey.id())
        );
    }

    public byte[] exportCsv(Long surveyId) {
        SurveyRecord survey = requireSurvey(surveyId);
        List<SurveyRawResponse> responses = repository.listResponses(survey.id(), 1, 10000).stream().map(this::toRawResponse).toList();
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Response ID,Survey,Registration ID,Name,Phone,Submitted At,Answers\n");
        for (SurveyRawResponse response : responses) {
            csv.append(csvCell(response.id())).append(',')
                    .append(csvCell(survey.title())).append(',')
                    .append(csvCell(response.registrationId())).append(',')
                    .append(csvCell(response.respondentName())).append(',')
                    .append(csvCell(response.respondentPhone())).append(',')
                    .append(csvCell(String.valueOf(response.submittedAt()))).append(',')
                    .append(csvCell(answersText(response.answers()))).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private SurveyResponse getSurvey(Long surveyId) {
        return repository.findSurvey(surveyId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Survey not found"));
    }

    private SurveyQuestionResponse getQuestion(Long questionId) {
        return repository.findQuestion(questionId)
                .map(this::toQuestionResponse)
                .orElseThrow(() -> new NotFoundException("Survey question not found"));
    }

    private SurveyOptionResponse getOption(Long optionId) {
        return repository.findOption(optionId)
                .map(this::toOptionResponse)
                .orElseThrow(() -> new NotFoundException("Survey option not found"));
    }

    private SurveyRecord requireSurvey(Long surveyId) {
        return repository.findSurvey(surveyId)
                .orElseThrow(() -> new NotFoundException("Survey not found"));
    }

    private SurveyQuestionRecord requireQuestion(Long questionId) {
        return repository.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("Survey question not found"));
    }

    private SurveyOptionRecord requireOption(Long optionId) {
        return repository.findOption(optionId)
                .orElseThrow(() -> new NotFoundException("Survey option not found"));
    }

    private void requireActivity(Long activityId) {
        if (!repository.activityExists(activityId)) {
            throw new NotFoundException("Activity not found");
        }
    }

    private void requireChoiceQuestion(SurveyQuestionRecord question) {
        SurveyQuestionType type = parseQuestionType(question.type());
        if (type != SurveyQuestionType.SINGLE_CHOICE && type != SurveyQuestionType.MULTIPLE_CHOICE) {
            throw new BusinessRuleException("INVALID_REQUEST", "Only choice questions can have options", 400);
        }
    }

    private SurveyRecord requireSurveyForActivity(Long activityId) {
        requireActivity(activityId);
        return repository.findSurveyByActivityId(activityId)
                .orElseThrow(() -> new NotFoundException("Survey not found"));
    }

    private void requirePublished(SurveyRecord survey) {
        if (SurveyStatus.valueOf(survey.status()) != SurveyStatus.PUBLISHED) {
            throw new BusinessRuleException("INVALID_STATE", "Survey is not published", 409);
        }
    }

    private SurveyRegistrationRecord requireEligibleRegistration(Long activityId, String phone) {
        SurveyRegistrationRecord registration = repository.findRegistrationForSurvey(activityId, phone)
                .orElseThrow(() -> new NotFoundException("Registration not found"));
        if (!"REGISTERED".equals(registration.status())) {
            throw new BusinessRuleException("INVALID_STATE", "Registration is not active", 409);
        }
        if (!"CHECKED_IN".equals(registration.checkInStatus())) {
            throw new BusinessRuleException("NOT_CHECKED_IN", "Registration is not checked in", 409);
        }
        return registration;
    }

    private void ensureNoResponses(Long surveyId) {
        if (repository.hasResponses(surveyId)) {
            throw new BusinessRuleException("INVALID_STATE", "Submitted survey cannot be edited", 409);
        }
    }

    private Map<Long, SurveyAnswerRequest> answersByQuestion(List<SurveyAnswerRequest> answers) {
        Map<Long, SurveyAnswerRequest> result = new HashMap<>();
        if (answers == null) {
            return result;
        }
        for (SurveyAnswerRequest answer : answers) {
            result.put(parseId(answer.questionId(), "Question id is invalid"), answer);
        }
        return result;
    }

    private void requireRequiredAnswers(List<SurveyQuestionRecord> questions, Map<Long, SurveyAnswerRequest> answersByQuestion) {
        for (SurveyQuestionRecord question : questions) {
            if (Boolean.TRUE.equals(question.required()) && !answersByQuestion.containsKey(question.id())) {
                throw new BusinessRuleException("INVALID_REQUEST", "Required question is missing", 400);
            }
        }
    }

    private void insertAnswer(Long responseId, SurveyQuestionRecord question, SurveyAnswerRequest answer, LocalDateTime submittedAt) {
        SurveyQuestionType type = parseQuestionType(question.type());
        Long optionId = null;
        String optionIdsJson = null;
        BigDecimal numericValue = null;
        String textValue = null;
        if (type == SurveyQuestionType.SINGLE_CHOICE) {
            optionId = parseId(answer.optionId(), "Option id is invalid");
            requireOptionForQuestion(optionId, question.id());
        } else if (type == SurveyQuestionType.MULTIPLE_CHOICE) {
            List<Long> optionIds = answer.optionIds() == null ? List.of() : answer.optionIds().stream()
                    .map(value -> parseId(value, "Option id is invalid"))
                    .toList();
            for (Long selectedOptionId : optionIds) {
                requireOptionForQuestion(selectedOptionId, question.id());
            }
            optionIdsJson = toJsonArray(optionIds);
        } else if (type == SurveyQuestionType.RATING) {
            numericValue = answer.numericValue();
            if (numericValue == null) {
                throw new BusinessRuleException("INVALID_REQUEST", "Rating value is required", 400);
            }
        } else if (type == SurveyQuestionType.TEXT) {
            textValue = answer.textValue();
        }
        repository.insertAnswer(IdWorker.getId(), responseId, question.id(), optionId, optionIdsJson, numericValue, textValue, submittedAt);
    }

    private void requireOptionForQuestion(Long optionId, Long questionId) {
        SurveyOptionRecord option = requireOption(optionId);
        if (!option.questionId().equals(questionId)) {
            throw new NotFoundException("Survey option not found");
        }
    }

    private SurveySubmitResponse toSubmitResponse(SurveyResponseRecord response) {
        return new SurveySubmitResponse(
                String.valueOf(response.id()),
                String.valueOf(response.surveyId()),
                String.valueOf(response.registrationId()),
                response.respondentName(),
                response.respondentPhone(),
                response.submittedAt()
        );
    }

    private SurveyRawResponse toRawResponse(SurveyResponseRecord response) {
        return new SurveyRawResponse(
                String.valueOf(response.id()),
                String.valueOf(response.surveyId()),
                String.valueOf(response.registrationId()),
                response.respondentName(),
                response.respondentPhone(),
                response.submittedAt(),
                repository.listAnswersForResponse(response.id()).stream().map(this::toRawAnswerResponse).toList()
        );
    }

    private SurveyRawAnswerResponse toRawAnswerResponse(SurveyAnswerRecord answer) {
        return new SurveyRawAnswerResponse(
                String.valueOf(answer.questionId()),
                answer.questionTitle(),
                answer.questionType(),
                answer.optionId() == null ? null : String.valueOf(answer.optionId()),
                answer.optionLabel(),
                parseJsonIdList(answer.optionIdsJson()),
                answer.numericValue(),
                answer.textValue()
        );
    }

    private SurveyQuestionStatisticsResponse toQuestionStatistics(SurveyQuestionRecord question) {
        List<SurveyOptionStatisticsResponse> optionStats = repository.listOptions(question.id()).stream()
                .map(option -> new SurveyOptionStatisticsResponse(
                        String.valueOf(option.id()),
                        option.label(),
                        repository.countOptionSelected(option.id())
                ))
                .toList();
        return new SurveyQuestionStatisticsResponse(
                String.valueOf(question.id()),
                question.title(),
                question.type(),
                repository.countAnswers(question.id()),
                SurveyQuestionType.valueOf(question.type()) == SurveyQuestionType.RATING ? repository.averageRatingForQuestion(question.id()) : null,
                optionStats
        );
    }

    private Long parseId(String value, String message) {
        try {
            return Long.valueOf(value);
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("INVALID_REQUEST", message, 400);
        }
    }

    private String toJsonArray(List<Long> ids) {
        return ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    private List<String> parseJsonIdList(String json) {
        if (json == null || json.length() < 2) {
            return List.of();
        }
        return java.util.Arrays.stream(json.substring(1, json.length() - 1).split(","))
                .map(value -> value.replace("\"", "").trim())
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String answersText(List<SurveyRawAnswerResponse> answers) {
        return answers.stream()
                .map(answer -> answer.questionTitle() + "=" + answerValue(answer))
                .collect(Collectors.joining("; "));
    }

    private String answerValue(SurveyRawAnswerResponse answer) {
        if (answer.optionLabel() != null) {
            return answer.optionLabel();
        }
        if (answer.optionIds() != null && !answer.optionIds().isEmpty()) {
            return String.join("|", answer.optionIds());
        }
        if (answer.numericValue() != null) {
            return String.valueOf(answer.numericValue());
        }
        return answer.textValue() == null ? "" : answer.textValue();
    }

    private String csvCell(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private SurveyQuestionType parseQuestionType(String type) {
        try {
            return SurveyQuestionType.valueOf(type);
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("INVALID_REQUEST", "Survey question type is not supported", 400);
        }
    }

    private SurveyQuestionRequest normalizedQuestionRequest(SurveyQuestionRequest request, SurveyQuestionType type) {
        return new SurveyQuestionRequest(request.title(), type.name(), request.required(), request.sortOrder());
    }

    private SurveyResponse toResponse(SurveyRecord survey) {
        List<SurveyQuestionResponse> questions = repository.listQuestions(survey.id())
                .stream()
                .map(this::toQuestionResponse)
                .toList();
        return new SurveyResponse(
                String.valueOf(survey.id()),
                String.valueOf(survey.activityId()),
                survey.title(),
                survey.description(),
                survey.status(),
                questions,
                survey.createdAt(),
                survey.updatedAt()
        );
    }

    private SurveyQuestionResponse toQuestionResponse(SurveyQuestionRecord question) {
        return new SurveyQuestionResponse(
                String.valueOf(question.id()),
                String.valueOf(question.surveyId()),
                question.title(),
                question.type(),
                question.required(),
                question.sortOrder(),
                repository.listOptions(question.id()).stream().map(this::toOptionResponse).toList(),
                question.createdAt(),
                question.updatedAt()
        );
    }

    private SurveyOptionResponse toOptionResponse(SurveyOptionRecord option) {
        return new SurveyOptionResponse(
                String.valueOf(option.id()),
                String.valueOf(option.questionId()),
                option.label(),
                option.value(),
                option.sortOrder(),
                option.createdAt(),
                option.updatedAt()
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
