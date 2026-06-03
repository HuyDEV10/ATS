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
import com.dacn.ATS.module.verification.entity.SkillVerification;
import com.dacn.ATS.module.verification.enums.VerificationStatus;
import com.dacn.ATS.module.verification.service.VerificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AiScoringServiceImpl implements AiScoringService {

    private static final String ALGORITHM_VERSION = "RULE_WEIGHTED_V1_SKILL_EVIDENCE_BONUS";

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
    private final VerificationService verificationService;

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
            ObjectMapper objectMapper,
            VerificationService verificationService) {
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
        this.verificationService = verificationService;
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
                "Mức độ khớp tổng thể trước Skill Evidence Bonus: " + overall + "%"));

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

        /*
         * Skill Evidence Bonus:
         * Ứng viên có thể cung cấp certificate/GitHub/portfolio link khi apply.
         * Hệ thống lưu các link này thành SkillVerification.
         * Khi HR chạy AI Screening, hệ thống cộng thêm bonus nếu evidence:
         * - có confidence cao,
         * - không bị reject,
         * - liên quan đến job đang apply.
         */
        SkillEvidenceBonus evidenceBonus = calculateSkillEvidenceBonus(candidate, job);

        if (evidenceBonus.points() > 0) {
            int scoreBeforeBonus = result.getOverallScore();
            int finalScore = clampScore(scoreBeforeBonus + evidenceBonus.points());

            result.setOverallScore(finalScore);
            result.setRecommendation(buildRecommendation(finalScore));

            List<String> updatedStrengths = new ArrayList<>(
                    result.getStrengths() == null ? List.of() : result.getStrengths());

            updatedStrengths.add("Skill Evidence Bonus: +" + evidenceBonus.points()
                    + " điểm từ minh chứng kỹ năng hợp lệ/liên quan đến job.");
            updatedStrengths.add("Final Score sau bonus: " + scoreBeforeBonus + " + "
                    + evidenceBonus.points() + " = " + finalScore);

            result.setStrengths(updatedStrengths);
        }

        saveScore(app, candidate, resume, result, prompt.getVersion(), evidenceBonus);
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
            String promptVersion,
            SkillEvidenceBonus evidenceBonus) {

        ApplicationScore score = new ApplicationScore();

        score.setCompanyId(app.getCompanyId());
        score.setApplicationId(app.getId());
        score.setJobId(app.getJobId());
        score.setCandidateId(candidate.getId());
        score.setResumeId(resume != null ? resume.getId() : null);

        score.setOverallScore(result.getOverallScore());
        score.setSkillScore(result.getSkillScore());
        score.setExperienceScore(result.getExperienceScore());
        score.setKeywordScore(result.getKeywordScore());

        score.setMatchedSkills(joinList(result.getMatchedSkills()));
        score.setMissingSkills(joinList(result.getMissingSkills()));
        score.setStrengths(joinLines(result.getStrengths()));
        score.setWeaknesses(joinLines(result.getWeaknesses()));
        score.setRecommendation(result.getRecommendation());
        score.setInterviewQuestions(joinLines(result.getInterviewQuestions()));
        score.setAlgorithmVersion(ALGORITHM_VERSION);
        score.setPromptVersion(promptVersion);

        score.setScoreTime(LocalDateTime.now());
        score.setDeleted(0);

        scoreMapper.insert(score);
        saveScoreDetails(score.getId(), result, evidenceBonus);
    }

    private void saveScoreDetails(Long scoreId, CvScoreResult result, SkillEvidenceBonus evidenceBonus) {
        saveScoreDetail(
                scoreId,
                "required_and_matching_skills",
                45,
                result.getSkillScore(),
                joinList(result.getMatchedSkills()),
                "Đánh giá mức độ khớp giữa kỹ năng trong JD và kỹ năng/CV của ứng viên");

        saveScoreDetail(
                scoreId,
                "keyword_coverage",
                35,
                result.getKeywordScore(),
                joinList(result.getMissingSkills()),
                "Đo tỷ lệ từ khóa quan trọng còn thiếu hoặc đã xuất hiện trong hồ sơ");

        saveScoreDetail(
                scoreId,
                "experience",
                20,
                result.getExperienceScore(),
                "experience_score=" + result.getExperienceScore(),
                "Điểm kinh nghiệm dựa trên số năm kinh nghiệm khai báo và thông tin trong hồ sơ");

        if (evidenceBonus != null) {
            saveScoreDetail(
                    scoreId,
                    "skill_evidence_bonus",
                    0,
                    evidenceBonus.points(),
                    evidenceBonus.summary(),
                    "Điểm cộng từ certificate/GitHub/portfolio evidence do ứng viên cung cấp khi apply job. "
                            + "Bonus chỉ được cộng nếu evidence có confidence cao, không bị reject và liên quan đến job. "
                            + "Tổng bonus tối đa +10 điểm.");
        }
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

        for (String skill : safeList(matched).stream().limit(3).toList()) {
            questions.add("Bạn hãy mô tả một dự án thực tế đã sử dụng " + skill + "?");
        }

        for (String skill : safeList(missing).stream().limit(2).toList()) {
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

    private SkillEvidenceBonus calculateSkillEvidenceBonus(Candidate candidate, Job job) {
        if (candidate == null || candidate.getId() == null) {
            return new SkillEvidenceBonus(0, "Không có candidate để kiểm tra skill evidence.");
        }

        List<SkillVerification> verifications;

        try {
            verifications = verificationService.listByCandidate(candidate.getId());
        } catch (Exception e) {
            return new SkillEvidenceBonus(0, "Không thể đọc skill evidence của ứng viên: " + e.getMessage());
        }

        if (verifications == null || verifications.isEmpty()) {
            return new SkillEvidenceBonus(0, "Ứng viên chưa cung cấp skill evidence.");
        }

        String jobText = normalizeForEvidence(
                safe(job.getTitle()) + " "
                        + safe(job.getDescription()) + " "
                        + safe(job.getDepartment()));

        int totalBonus = 0;
        List<String> used = new ArrayList<>();
        List<String> ignored = new ArrayList<>();

        for (SkillVerification verification : verifications) {
            if (verification == null) {
                continue;
            }

            String skill = verification.getSkillName();
            int confidence = nullToZero(verification.getConfidenceScore());
            String status = verification.getStatus();

            boolean relatedToJob = isEvidenceRelatedToJob(skill, jobText);

            if (!relatedToJob) {
                ignored.add(formatEvidenceLine(verification, 0, "Ignored: skill is not related to this job"));
                continue;
            }

            int bonus = calculateOneEvidenceBonus(status, confidence);

            if (bonus > 0) {
                totalBonus += bonus;
                used.add(formatEvidenceLine(verification, bonus, "Used"));
            } else {
                ignored.add(
                        formatEvidenceLine(verification, 0, "Ignored: low confidence, pending too weak, or rejected"));
            }

            if (totalBonus >= 10) {
                totalBonus = 10;
                break;
            }
        }

        StringBuilder summary = new StringBuilder();

        summary.append("Skill Evidence Bonus: +").append(totalBonus).append("\n");
        summary.append("Rule: VERIFIED + confidence>=80 => +3; VERIFIED + confidence>=60 => +2; ")
                .append("PENDING/SUBMITTED + confidence>=80 => +1; REJECTED/not related => +0. Max bonus: +10.\n");

        if (!used.isEmpty()) {
            summary.append("\nEvidence used:\n");
            for (String item : used) {
                summary.append("- ").append(item).append("\n");
            }
        }

        if (!ignored.isEmpty()) {
            summary.append("\nEvidence ignored:\n");
            for (String item : ignored) {
                summary.append("- ").append(item).append("\n");
            }
        }

        if (used.isEmpty() && ignored.isEmpty()) {
            summary.append("\nKhông có skill evidence phù hợp để cộng điểm.");
        }

        return new SkillEvidenceBonus(totalBonus, summary.toString());
    }

    private int calculateOneEvidenceBonus(String status, int confidence) {
        if (VerificationStatus.REJECTED.name().equals(status)) {
            return 0;
        }

        if (VerificationStatus.VERIFIED.name().equals(status)) {
            if (confidence >= 80) {
                return 3;
            }

            if (confidence >= 60) {
                return 2;
            }

            return 0;
        }

        /*
         * SUBMITTED / PENDING_VERIFICATION:
         * Chưa được HR xác nhận nên chỉ cộng nhẹ nếu confidence cao.
         */
        if (VerificationStatus.SUBMITTED.name().equals(status)
                || VerificationStatus.PENDING_VERIFICATION.name().equals(status)) {
            if (confidence >= 80) {
                return 1;
            }
        }

        return 0;
    }

    private boolean isEvidenceRelatedToJob(String skill, String normalizedJobText) {
        if (!StringUtils.hasText(skill) || !StringUtils.hasText(normalizedJobText)) {
            return false;
        }

        String normalizedSkill = normalizeForEvidence(skill);

        if (!StringUtils.hasText(normalizedSkill)) {
            return false;
        }

        return normalizedJobText.contains(normalizedSkill);
    }

    private String formatEvidenceLine(SkillVerification verification, int bonus, String note) {
        return safe(verification.getSkillName())
                + " | provider=" + safe(verification.getProvider())
                + " | status=" + safe(verification.getStatus())
                + " | confidence=" + nullToZero(verification.getConfidenceScore())
                + " | bonus=+" + bonus
                + " | " + note;
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

    private String normalizeForEvidence(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replace("-", " ")
                .replace("_", " ")
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

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String joinList(List<String> values) {
        return String.join(", ", safeList(values));
    }

    private String joinLines(List<String> values) {
        return String.join("\n", safeList(values));
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private record SkillEvidenceBonus(
            int points,
            String summary) {
    }

    private record VerificationResult(
            String status,
            int score,
            String summary) {
    }
}