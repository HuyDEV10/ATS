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

    public PublicApplyServiceImpl(
            JobService jobService,
            ResumeService resumeService,
            CandidateService candidateService,
            JobApplicationMapper jobApplicationMapper) {
        this.jobService = jobService;
        this.resumeService = resumeService;
        this.candidateService = candidateService;
        this.jobApplicationMapper = jobApplicationMapper;
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

        JobApplication application = new JobApplication();
        application.setCompanyId(job.getCompanyId());
        application.setJobId(job.getId());
        application.setCandidateId(candidate.getId());
        application.setResumeId(resume.getId());
        application.setStatus(ApplicationStatus.PENDING.name());
        application.setVerificationStatus(resolveVerificationStatus(resume));
        application.setMismatchScore(0);
        application.setMismatchSummary(buildMismatchSummary(resume));
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
        result.setMessage(buildResultMessage(resume));

        return result;
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

    private String buildMismatchSummary(Resume resume) {
        if ("PARSED".equals(resume.getParseStatus())) {
            return "CV uploaded and parsed successfully.";
        }

        if ("FAILED".equals(resume.getParseStatus())) {
            return "CV uploaded but parsing failed. HR should review this CV manually.";
        }

        return "CV uploaded. Verification is pending.";
    }

    private String buildResultMessage(Resume resume) {
        if ("PARSED".equals(resume.getParseStatus())) {
            return "Your application has been submitted successfully. Your CV was parsed successfully.";
        }

        if ("FAILED".equals(resume.getParseStatus())) {
            return "Your application has been submitted successfully, but your CV could not be parsed automatically. Our HR team will review it manually.";
        }

        return "Your application has been submitted successfully.";
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}