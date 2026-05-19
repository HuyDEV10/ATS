package com.dacn.ATS.module.ai.service.impl;

import com.dacn.ATS.module.ai.client.AiClient;
import com.dacn.ATS.module.ai.client.AiCompletionRequest;
import com.dacn.ATS.module.ai.client.AiCompletionResponse;
import com.dacn.ATS.module.ai.dto.AiScoringPrompt;
import com.dacn.ATS.module.ai.dto.CvScoreResult;
import com.dacn.ATS.module.ai.entity.ApplicationScore;
import com.dacn.ATS.module.ai.entity.ApplicationScoreDetail;
import com.dacn.ATS.module.ai.mapper.ApplicationScoreDetailMapper;
import com.dacn.ATS.module.ai.mapper.ApplicationScoreMapper;
import com.dacn.ATS.module.ai.service.AiScoringPromptBuilder;
import com.dacn.ATS.module.ai.service.AiScoringService;
import com.dacn.ATS.module.ai.service.CvParserService;
import com.dacn.ATS.module.ai.service.SkillExtractionService;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.enums.ApplicationStatus;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.mapper.JobMapper;
import com.dacn.ATS.module.resume.entity.Resume;
import com.dacn.ATS.module.resume.mapper.ResumeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AiScoringServiceImpl implements AiScoringService {

    private static final String ALGORITHM_VERSION = "RULE_WEIGHTED_V1";

    private final JobApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final CandidateMapper candidateMapper;
    private final ResumeMapper resumeMapper;
    private final ApplicationScoreMapper scoreMapper;
    private final ApplicationScoreDetailMapper scoreDetailMapper;
    private final CvParserService cvParserService;
    private final SkillExtractionService skillExtractionService;
    private final AiScoringPromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public AiScoringServiceImpl(
            JobApplicationMapper applicationMapper,
            JobMapper jobMapper,
            CandidateMapper candidateMapper,
            ResumeMapper resumeMapper,
            ApplicationScoreMapper scoreMapper,
            ApplicationScoreDetailMapper scoreDetailMapper,
            CvParserService cvParserService,
            SkillExtractionService skillExtractionService,
            AiScoringPromptBuilder promptBuilder,
            AiClient aiClient,
            ObjectMapper objectMapper) {
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.candidateMapper = candidateMapper;
        this.resumeMapper = resumeMapper;
        this.scoreMapper = scoreMapper;
        this.scoreDetailMapper = scoreDetailMapper;
        this.cvParserService = cvParserService;
        this.skillExtractionService = skillExtractionService;
        this.promptBuilder = promptBuilder;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public CvScoreResult scoreCv(Long applicationId) {

        JobApplication app = applicationMapper.selectById(applicationId);

        if (app == null) {
            throw new RuntimeException("Application not found");
        }

        Job job = jobMapper.selectById(app.getJobId());
        Candidate candidate = candidateMapper.selectById(app.getCandidateId());

        if (job == null || candidate == null) {
            throw new RuntimeException("Job or candidate not found");
        }

        Resume resume = null;
        String cvText = "";

        Long resumeId = app.getResumeId() != null
                ? app.getResumeId()
                : candidate.getResumeId();

        if (resumeId != null) {
            resume = resumeMapper.selectById(resumeId);

            if (resume != null) {
                if (StringUtils.hasText(resume.getParsedText())) {
                    cvText = resume.getParsedText();
                } else if (StringUtils.hasText(resume.getFilePath())) {
                    cvText = cvParserService.parseToText(resume.getFilePath());
                }
            }
        }

        VerificationResult verificationResult = verifyCandidateWithResume(candidate, resume, cvText);

        app.setResumeId(resumeId);
        app.setVerificationStatus(verificationResult.status());
        app.setMismatchScore(verificationResult.score());
        app.setMismatchSummary(verificationResult.summary());
        app.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(app);

        String jobText = safe(job.getTitle()) + " "
                + safe(job.getDescription()) + " "
                + safe(job.getDepartment());

        String candidateText = safe(candidate.getSkills()) + " "
                + safe(candidate.getExperienceYears()) + " "
                + cvText;

        Set<String> jobKeywords = skillExtractionService.extractSkills(jobText);
        Set<String> cvKeywords = skillExtractionService.extractSkills(candidateText);

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String keyword : jobKeywords) {
            if (cvKeywords.contains(keyword)) {
                matched.add(keyword);
            } else {
                missing.add(keyword);
            }
        }

        int keywordScore = jobKeywords.isEmpty()
                ? 0
                : (int) Math.round((matched.size() * 100.0) / jobKeywords.size());

        int skillScore = Math.min(100, keywordScore + matched.size() * 2);

        int experienceScore = 50;

        if (candidate.getExperienceYears() != null) {
            experienceScore = Math.min(100, candidate.getExperienceYears() * 15);
        }

        int overall = (int) Math.round(
                skillScore * 0.45
                        + keywordScore * 0.35
                        + experienceScore * 0.20);

        CvScoreResult result = new CvScoreResult();

        result.setOverallScore(overall);
        result.setSkillScore(skillScore);
        result.setExperienceScore(experienceScore);
        result.setKeywordScore(keywordScore);
        result.setMatchedSkills(matched);
        result.setMissingSkills(missing);

        result.setStrengths(List.of(
                "Có " + matched.size() + " kỹ năng/từ khóa phù hợp với JD",
                "Mức độ khớp tổng thể: " + overall + "%"));

        result.setWeaknesses(
                missing.isEmpty()
                        ? List.of("Chưa phát hiện điểm thiếu lớn")
                        : List.of("Thiếu một số kỹ năng quan trọng: " + String.join(", ", missing)));

        result.setRecommendation(buildRecommendation(overall));
        result.setInterviewQuestions(buildInterviewQuestions(matched, missing));

        result.setVerificationStatus(verificationResult.status());
        result.setMismatchScore(verificationResult.score());
        result.setMismatchSummary(verificationResult.summary());

        AiScoringPrompt prompt = promptBuilder.build(job, candidate, cvText);
        AiCompletionResponse aiResponse = completeWithAi(prompt);

        if (aiResponse.isSuccessful()) {
            result = tryUseAiResult(result, aiResponse.getContent());

            result.setVerificationStatus(verificationResult.status());
            result.setMismatchScore(verificationResult.score());
            result.setMismatchSummary(verificationResult.summary());
        }

        saveScore(app, candidate, resume, result, prompt.getVersion());
        markApplicationScreened(app);

        return result;
    }

    private VerificationResult verifyCandidateWithResume(Candidate candidate, Resume resume, String cvText) {
        if (resume == null) {
            return new VerificationResult(
                    "NO_RESUME",
                    0,
                    "Ứng viên chưa có CV đính kèm.");
        }

        if (!StringUtils.hasText(cvText)) {
            return new VerificationResult(
                    "CV_PARSE_FAILED",
                    80,
                    "Không thể đọc nội dung CV. HR cần kiểm tra file thủ công.");
        }

        String normalizedCvText = normalize(cvText);
        int score = 0;
        List<String> notes = new ArrayList<>();

        if (StringUtils.hasText(candidate.getEmail())
                && !normalizedCvText.contains(normalize(candidate.getEmail()))) {
            score += 45;
            notes.add("Email trong form không xuất hiện trong CV");
        }

        String formPhone = digitsOnly(candidate.getPhone());

        if (StringUtils.hasText(formPhone)
                && !digitsOnly(normalizedCvText).contains(formPhone)) {
            score += 30;
            notes.add("Số điện thoại trong form không xuất hiện trong CV");
        }

        if (StringUtils.hasText(candidate.getName())
                && !normalizedCvText.contains(normalize(candidate.getName()))) {
            score += 15;
            notes.add("Họ tên trong form không xuất hiện trong CV");
        }

        int totalSkills = countSkills(candidate.getSkills());
        int matchedSkills = countMatchedSkills(candidate.getSkills(), normalizedCvText);

        if (totalSkills > 0 && matchedSkills == 0) {
            score += 10;
            notes.add("Kỹ năng khai báo trong form không tìm thấy trong CV");
        }

        score = Math.min(score, 100);

        if (notes.isEmpty()) {
            return new VerificationResult(
                    "VERIFIED",
                    0,
                    "Thông tin trong form khớp với nội dung CV.");
        }

        String status = score >= 45
                ? "IDENTITY_CONFLICT"
                : "NEEDS_REVIEW";

        return new VerificationResult(
                status,
                score,
                String.join("; ", notes));
    }

    private void saveScore(
            JobApplication app,
            Candidate candidate,
            Resume resume,
            CvScoreResult result,
            String promptVersion) {

        ApplicationScore score = new ApplicationScore();

        score.setApplicationId(app.getId());
        score.setJobId(app.getJobId());
        score.setCandidateId(candidate.getId());
        score.setResumeId(resume != null ? resume.getId() : null);

        score.setOverallScore(result.getOverallScore());
        score.setSkillScore(result.getSkillScore());
        score.setExperienceScore(result.getExperienceScore());
        score.setKeywordScore(result.getKeywordScore());

        score.setMatchedSkills(String.join(", ", result.getMatchedSkills()));
        score.setMissingSkills(String.join(", ", result.getMissingSkills()));
        score.setStrengths(String.join("\n", result.getStrengths()));
        score.setWeaknesses(String.join("\n", result.getWeaknesses()));
        score.setRecommendation(result.getRecommendation());
        score.setInterviewQuestions(String.join("\n", result.getInterviewQuestions()));
        score.setAlgorithmVersion(ALGORITHM_VERSION);
        score.setPromptVersion(promptVersion);

        score.setScoreTime(LocalDateTime.now());
        score.setDeleted(0);

        scoreMapper.insert(score);
        saveScoreDetails(score.getId(), result);
    }

    private void saveScoreDetails(Long scoreId, CvScoreResult result) {
        saveScoreDetail(
                scoreId,
                "required_and_matching_skills",
                45,
                result.getSkillScore(),
                String.join(", ", result.getMatchedSkills()),
                "Đánh giá mức độ khớp giữa kỹ năng trong JD và kỹ năng/CV của ứng viên");

        saveScoreDetail(
                scoreId,
                "keyword_coverage",
                35,
                result.getKeywordScore(),
                String.join(", ", result.getMissingSkills()),
                "Đo tỷ lệ từ khóa quan trọng còn thiếu hoặc đã xuất hiện trong hồ sơ");

        saveScoreDetail(
                scoreId,
                "experience",
                20,
                result.getExperienceScore(),
                "experience_score=" + result.getExperienceScore(),
                "Điểm kinh nghiệm dựa trên số năm kinh nghiệm khai báo và thông tin trong hồ sơ");
    }

    private void saveScoreDetail(
            Long scoreId,
            String criterion,
            int weight,
            int score,
            String evidence,
            String explanation) {

        ApplicationScoreDetail detail = new ApplicationScoreDetail();

        detail.setApplicationScoreId(scoreId);
        detail.setCriterion(criterion);
        detail.setWeight(weight);
        detail.setScore(score);
        detail.setEvidence(evidence);
        detail.setExplanation(explanation);
        detail.setCreateTime(LocalDateTime.now());
        detail.setDeleted(0);

        scoreDetailMapper.insert(detail);
    }

    private AiCompletionResponse completeWithAi(AiScoringPrompt prompt) {
        AiCompletionRequest request = new AiCompletionRequest();

        request.setSystemPrompt(prompt.getSystemPrompt());
        request.setUserPrompt(prompt.getUserPrompt());
        request.setResponseSchema(prompt.getResponseSchema());
        request.setPromptVersion(prompt.getVersion());

        return aiClient.complete(request);
    }

    private void markApplicationScreened(JobApplication app) {
        if (ApplicationStatus.PENDING.name().equals(app.getStatus())) {
            app.setStatus(ApplicationStatus.AI_SCREENED.name());
            app.setUpdateTime(LocalDateTime.now());
            applicationMapper.updateById(app);
        }
    }

    private String buildRecommendation(int score) {
        if (score >= 80) {
            return "STRONG_MATCH - Nên đưa vào vòng phỏng vấn";
        }

        if (score >= 60) {
            return "POTENTIAL_MATCH - Có thể cân nhắc phỏng vấn";
        }

        if (score >= 40) {
            return "WEAK_MATCH - Cần HR xem xét thêm";
        }

        return "NOT_MATCH - Chưa phù hợp với vị trí";
    }

    private List<String> buildInterviewQuestions(
            List<String> matched,
            List<String> missing) {

        List<String> questions = new ArrayList<>();

        for (String skill : matched.stream().limit(3).toList()) {
            questions.add("Bạn hãy mô tả một dự án thực tế đã sử dụng " + skill + "?");
        }

        for (String skill : missing.stream().limit(2).toList()) {
            questions.add("Bạn đã từng tiếp cận hoặc học về " + skill + " chưa?");
        }

        if (questions.isEmpty()) {
            questions.add("Bạn hãy giới thiệu kinh nghiệm phù hợp nhất với vị trí này?");
        }

        return questions;
    }

    private CvScoreResult tryUseAiResult(CvScoreResult fallback, String aiContent) {
        if (aiContent == null || aiContent.isBlank()) {
            return fallback;
        }

        try {
            JsonNode root = objectMapper.readTree(extractJson(aiContent));

            CvScoreResult result = new CvScoreResult();

            result.setOverallScore(clampScore(root.path("overallScore").asInt(fallback.getOverallScore())));
            result.setSkillScore(clampScore(root.path("skillScore").asInt(fallback.getSkillScore())));
            result.setExperienceScore(clampScore(root.path("experienceScore").asInt(fallback.getExperienceScore())));
            result.setKeywordScore(clampScore(root.path("keywordScore").asInt(fallback.getKeywordScore())));

            result.setMatchedSkills(readStringList(root.path("matchedSkills"), fallback.getMatchedSkills()));
            result.setMissingSkills(readStringList(root.path("missingSkills"), fallback.getMissingSkills()));
            result.setStrengths(readStringList(root.path("strengths"), fallback.getStrengths()));
            result.setWeaknesses(readStringList(root.path("weaknesses"), fallback.getWeaknesses()));
            result.setInterviewQuestions(
                    readStringList(root.path("interviewQuestions"), fallback.getInterviewQuestions()));

            String recommendation = root.path("recommendation").asText(fallback.getRecommendation());
            result.setRecommendation(recommendation);

            result.setVerificationStatus(fallback.getVerificationStatus());
            result.setMismatchScore(fallback.getMismatchScore());
            result.setMismatchSummary(fallback.getMismatchSummary());

            return result;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String extractJson(String value) {
        String trimmed = value.trim();

        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    private List<String> readStringList(JsonNode node, List<String> fallback) {
        if (node == null || !node.isArray()) {
            return fallback == null ? List.of() : fallback;
        }

        List<String> values = new ArrayList<>();

        for (JsonNode item : node) {
            String text = item.asText();

            if (text != null && !text.isBlank()) {
                values.add(text);
            }
        }

        return values;
    }

    private int countMatchedSkills(String skills, String cvText) {
        if (!StringUtils.hasText(skills)) {
            return 0;
        }

        int matched = 0;

        for (String skill : skills.split(",")) {
            String normalizedSkill = normalize(skill);

            if (StringUtils.hasText(normalizedSkill) && cvText.contains(normalizedSkill)) {
                matched++;
            }
        }

        return matched;
    }

    private int countSkills(String skills) {
        if (!StringUtils.hasText(skills)) {
            return 0;
        }

        int count = 0;

        for (String skill : skills.split(",")) {
            if (StringUtils.hasText(skill)) {
                count++;
            }
        }

        return count;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String digitsOnly(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\D+", "");
    }

    private int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private record VerificationResult(
            String status,
            int score,
            String summary) {
    }
}