package com.dacn.ATS.module.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.enums.ApplicationStatus;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.service.CandidateService;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.service.JobService;
import com.dacn.ATS.module.portal.dto.PublicApplyRequest;
import com.dacn.ATS.module.portal.dto.PublicApplyResult;
import com.dacn.ATS.module.portal.service.PublicApplyService;
import com.dacn.ATS.module.resume.entity.Resume;
import com.dacn.ATS.module.resume.service.ResumeService;
import com.dacn.ATS.module.verification.dto.SkillEvidenceRequest;
import com.dacn.ATS.module.verification.enums.VerificationSourceType;
import com.dacn.ATS.module.verification.service.VerificationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class PublicApplyServiceImpl implements PublicApplyService {

    private final JobService jobService;
    private final ResumeService resumeService;
    private final CandidateService candidateService;
    private final JobApplicationMapper jobApplicationMapper;
    private final VerificationService verificationService;

    public PublicApplyServiceImpl(
            JobService jobService,
            ResumeService resumeService,
            CandidateService candidateService,
            JobApplicationMapper jobApplicationMapper,
            VerificationService verificationService) {
        this.jobService = jobService;
        this.resumeService = resumeService;
        this.candidateService = candidateService;
        this.jobApplicationMapper = jobApplicationMapper;
        this.verificationService = verificationService;
    }

    @Override
    @Transactional
    public PublicApplyResult applyToJob(Long jobId, PublicApplyRequest request) {
        validateRequest(request);

        Job job = jobService.getPublicPublishedJobById(jobId);

        if (job.getCompanyId() == null) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "This job is missing company information. Please contact the recruiter.");
        }

        checkDuplicateApplication(job.getId(), request.getEmail());

        Resume resume;

        try {
            resume = resumeService.uploadResumeForCompany(request.getFile(), job.getCompanyId());
        } catch (Exception e) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "CV upload failed: " + e.getMessage());
        }

        Candidate candidate = new Candidate();
        candidate.setName(request.getName().trim());
        candidate.setEmail(request.getEmail().trim());
        candidate.setPhone(normalizeOptional(request.getPhone()));
        candidate.setSkills(normalizeOptional(request.getSkills()));
        candidate.setExperienceYears(request.getExperienceYears() == null ? 0 : request.getExperienceYears());
        candidate.setResumeId(resume.getId());

        candidate = candidateService.createPublicCandidate(candidate, job.getCompanyId());

        // Đợt Skill Evidence:
        // Ứng viên có thể nhập tối đa 3 link minh chứng kỹ năng khi apply.
        // Mỗi link được lưu thành một SkillVerification record, status ban đầu do
        // VerificationService tự tính.
        int submittedEvidenceCount = submitSkillEvidenceFromApplyForm(candidate.getId(), request);

        JobApplication application = new JobApplication();
        application.setCompanyId(job.getCompanyId());
        application.setJobId(job.getId());
        application.setCandidateId(candidate.getId());
        application.setResumeId(resume.getId());
        application.setStatus(ApplicationStatus.PENDING.name());
        application.setVerificationStatus(resolveVerificationStatus(resume));
        application.setMismatchScore(0);
        application.setMismatchSummary(buildMismatchSummary(resume, submittedEvidenceCount));
        application.setApplicationDate(LocalDateTime.now());
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        application.setDeleted(0);

        jobApplicationMapper.insert(application);

        PublicApplyResult result = new PublicApplyResult();
        result.setJobId(job.getId());
        result.setCompanyId(job.getCompanyId());
        result.setCandidateId(candidate.getId());
        result.setResumeId(resume.getId());
        result.setApplicationId(application.getId());
        result.setCandidateName(candidate.getName());
        result.setCandidateEmail(candidate.getEmail());
        result.setJobTitle(job.getTitle());
        result.setParseStatus(resume.getParseStatus());
        result.setVerificationStatus(application.getVerificationStatus());
        result.setMessage(buildResultMessage(resume, submittedEvidenceCount));

        return result;
    }

    private int submitSkillEvidenceFromApplyForm(Long candidateId, PublicApplyRequest request) {
        int count = 0;

        if (submitOneEvidence(candidateId,
                request.getEvidenceSkill1(),
                request.getEvidenceType1(),
                request.getEvidenceProvider1(),
                request.getEvidenceUrl1())) {
            count++;
        }

        if (submitOneEvidence(candidateId,
                request.getEvidenceSkill2(),
                request.getEvidenceType2(),
                request.getEvidenceProvider2(),
                request.getEvidenceUrl2())) {
            count++;
        }

        if (submitOneEvidence(candidateId,
                request.getEvidenceSkill3(),
                request.getEvidenceType3(),
                request.getEvidenceProvider3(),
                request.getEvidenceUrl3())) {
            count++;
        }

        return count;
    }

    private boolean submitOneEvidence(
            Long candidateId,
            String skillName,
            String evidenceType,
            String provider,
            String url) {

        if (!StringUtils.hasText(skillName) && !StringUtils.hasText(url)) {
            return false;
        }

        if (!StringUtils.hasText(skillName) || !StringUtils.hasText(url)) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "Each skill evidence must include both skill name and evidence URL.");
        }

        String normalizedUrl = normalizeUrl(url);

        if (!isProbablyValidUrl(normalizedUrl)) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "Invalid evidence URL: " + url);
        }

        SkillEvidenceRequest evidenceRequest = new SkillEvidenceRequest();
        evidenceRequest.setCandidateId(candidateId);
        evidenceRequest.setDeclaredSkill(skillName.trim());
        evidenceRequest.setSourceType(resolveSourceType(evidenceType, provider, normalizedUrl));
        evidenceRequest.setSourceName(resolveSourceName(provider, normalizedUrl));
        evidenceRequest.setSourceUrl(normalizedUrl);
        evidenceRequest.setEvidenceText(buildEvidenceText(skillName, provider, evidenceType, normalizedUrl));

        verificationService.submitEvidence(evidenceRequest, null);

        return true;
    }

    private String normalizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        String trimmed = url.trim();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        return "https://" + trimmed;
    }

    private boolean isProbablyValidUrl(String url) {
        return StringUtils.hasText(url)
                && url.matches("^(https?://)[A-Za-z0-9.-]+\\.[A-Za-z]{2,}.*$");
    }

    private VerificationSourceType resolveSourceType(String evidenceType, String provider, String url) {
        String text = (safe(evidenceType) + " " + safe(provider) + " " + safe(url)).toLowerCase();

        if (text.contains("github.com") || text.contains("github")) {
            return VerificationSourceType.GITHUB;
        }

        if (text.contains("coursera.org") || text.contains("coursera")) {
            return VerificationSourceType.COURSERA;
        }

        if (text.contains("udemy.com") || text.contains("udemy")) {
            return VerificationSourceType.UDEMY;
        }

        if (text.contains("linkedin.com") || text.contains("linkedin")) {
            return VerificationSourceType.LINKEDIN_LEARNING;
        }

        if (text.contains("certificate") || text.contains("cert")) {
            return VerificationSourceType.CERTIFICATE_LINK;
        }

        return VerificationSourceType.OTHER;
    }

    private String resolveSourceName(String provider, String url) {
        if (StringUtils.hasText(provider)) {
            return provider.trim();
        }

        String lowerUrl = safe(url).toLowerCase();

        if (lowerUrl.contains("github.com")) {
            return "GitHub";
        }

        if (lowerUrl.contains("coursera.org")) {
            return "Coursera";
        }

        if (lowerUrl.contains("udemy.com")) {
            return "Udemy";
        }

        if (lowerUrl.contains("linkedin.com")) {
            return "LinkedIn";
        }

        return "Other";
    }

    private String buildEvidenceText(String skillName, String provider, String evidenceType, String url) {
        return "Submitted during public job application.\n"
                + "Skill: " + safe(skillName) + "\n"
                + "Evidence type: " + safe(evidenceType) + "\n"
                + "Provider: " + safe(provider) + "\n"
                + "URL: " + safe(url) + "\n"
                + "Status: Pending HR verification.";
    }

    private void validateRequest(PublicApplyRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Application form is required");
        }

        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Full name is required");
        }

        if (!StringUtils.hasText(request.getEmail())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Email is required");
        }

        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Email format is invalid");
        }

        if (request.getExperienceYears() != null && request.getExperienceYears() < 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Experience years cannot be negative");
        }

        MultipartFile file = request.getFile();

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "CV file is required");
        }

        String originalName = file.getOriginalFilename();

        if (!StringUtils.hasText(originalName)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "CV file name is invalid");
        }

        String lowerName = originalName.toLowerCase();

        if (!(lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx"))) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "CV must be a PDF, DOC or DOCX file");
        }

        long maxSize = 10L * 1024L * 1024L;

        if (file.getSize() > maxSize) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "CV must be smaller than 10MB");
        }
    }

    private void checkDuplicateApplication(Long jobId, String email) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(JobApplication::getJobId, jobId)
                .inSql(
                        JobApplication::getCandidateId,
                        "SELECT id FROM candidates WHERE email = '" + escapeSql(email.trim()) + "' AND deleted = 0");

        if (jobApplicationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "You have already applied to this job with this email");
        }
    }

    private String resolveVerificationStatus(Resume resume) {
        if ("PARSED".equals(resume.getParseStatus())) {
            return "PARSED";
        }

        if ("FAILED".equals(resume.getParseStatus())) {
            return "PARSE_FAILED";
        }

        return "NEEDS_REVIEW";
    }

    private String buildMismatchSummary(Resume resume, int evidenceCount) {
        StringBuilder builder = new StringBuilder();

        if ("PARSED".equals(resume.getParseStatus())) {
            builder.append("CV uploaded and parsed successfully.");
        } else if ("FAILED".equals(resume.getParseStatus())) {
            builder.append("CV uploaded but parsing failed. HR should review this CV manually.");
        } else {
            builder.append("CV uploaded. Verification is pending.");
        }

        if (evidenceCount > 0) {
            builder.append("\nSkill evidence submitted during apply: ")
                    .append(evidenceCount)
                    .append(" link(s).");
        }

        return builder.toString();
    }

    private String buildResultMessage(Resume resume, int evidenceCount) {
        StringBuilder builder = new StringBuilder();

        if ("PARSED".equals(resume.getParseStatus())) {
            builder.append("Your application has been submitted successfully. Your CV was parsed successfully.");
        } else if ("FAILED".equals(resume.getParseStatus())) {
            builder.append(
                    "Your application has been submitted successfully, but your CV could not be parsed automatically. Our HR team will review it manually.");
        } else {
            builder.append("Your application has been submitted successfully.");
        }

        if (evidenceCount > 0) {
            builder.append(" We also received ")
                    .append(evidenceCount)
                    .append(" skill evidence link(s) for HR review.");
        }

        return builder.toString();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}