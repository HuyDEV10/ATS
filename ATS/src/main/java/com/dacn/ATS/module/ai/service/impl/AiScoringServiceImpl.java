package com.dacn.ATS.module.ai.service.impl;

import com.dacn.ATS.module.ai.dto.CvScoreResult;
import com.dacn.ATS.module.ai.entity.ApplicationScore;
import com.dacn.ATS.module.ai.mapper.ApplicationScoreMapper;
import com.dacn.ATS.module.ai.service.AiScoringService;
import com.dacn.ATS.module.ai.service.CvParserService;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.mapper.JobMapper;
import com.dacn.ATS.module.resume.entity.Resume;
import com.dacn.ATS.module.resume.mapper.ResumeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AiScoringServiceImpl implements AiScoringService {

    private final JobApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final CandidateMapper candidateMapper;
    private final ResumeMapper resumeMapper;
    private final ApplicationScoreMapper scoreMapper;
    private final CvParserService cvParserService;

    public AiScoringServiceImpl(
            JobApplicationMapper applicationMapper,
            JobMapper jobMapper,
            CandidateMapper candidateMapper,
            ResumeMapper resumeMapper,
            ApplicationScoreMapper scoreMapper,
            CvParserService cvParserService) {
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.candidateMapper = candidateMapper;
        this.resumeMapper = resumeMapper;
        this.scoreMapper = scoreMapper;
        this.cvParserService = cvParserService;
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

        if (candidate.getResumeId() != null) {
            resume = resumeMapper.selectById(candidate.getResumeId());
            if (resume != null) {
                cvText = cvParserService.parseToText(resume.getFilePath());
            }
        }

        String jobText = safe(job.getTitle()) + " "
                + safe(job.getDescription()) + " "
                + safe(job.getDepartment());

        String candidateText = safe(candidate.getSkills()) + " "
                + safe(candidate.getExperienceYears()) + " "
                + cvText;

        Set<String> jobKeywords = extractKeywords(jobText);
        Set<String> cvKeywords = extractKeywords(candidateText);

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

        saveScore(app, candidate, resume, result);

        return result;
    }

    private void saveScore(
            JobApplication app,
            Candidate candidate,
            Resume resume,
            CvScoreResult result) {
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

        score.setScoreTime(LocalDateTime.now());
        score.setDeleted(0);

        scoreMapper.insert(score);
    }

    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();

        List<String> importantSkills = List.of(
                "java", "spring", "spring boot", "mysql", "sql",
                "html", "css", "javascript", "react", "vue",
                "python", "machine learning", "ai", "docker",
                "git", "api", "rest", "security", "thymeleaf",
                "mybatis", "database");

        String lower = safe(text).toLowerCase();

        for (String skill : importantSkills) {
            if (lower.contains(skill)) {
                keywords.add(skill);
            }
        }

        return keywords;
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

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }
}