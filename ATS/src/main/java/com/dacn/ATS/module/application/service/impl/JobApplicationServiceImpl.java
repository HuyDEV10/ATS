package com.dacn.ATS.module.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.enums.ApplicationStatus;
import com.dacn.ATS.module.application.enums.ApplicationStatusTransitionValidator;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
import com.dacn.ATS.module.application.service.JobApplicationService;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.mapper.JobMapper;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final CandidateMapper candidateMapper;

    public JobApplicationServiceImpl(
            JobApplicationMapper applicationMapper,
            JobMapper jobMapper,
            CandidateMapper candidateMapper) {
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.candidateMapper = candidateMapper;
    }

    @Override
    public JobApplication createApplication(JobApplication application) {
        Job job = jobMapper.selectById(application.getJobId());

        if (job == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Job not found");
        }

        Candidate candidate = candidateMapper.selectById(application.getCandidateId());

        if (candidate == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Candidate not found");
        }

        if (job.getCompanyId() == null) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "Job has no company scope");
        }

        if (candidate.getCompanyId() == null || !job.getCompanyId().equals(candidate.getCompanyId())) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "Candidate and job do not belong to the same company");
        }

        if (!CurrentUserUtil.isPlatformAdmin()) {
            Long currentCompanyId = CurrentUserUtil.getCurrentCompanyId();

            if (currentCompanyId == null || !currentCompanyId.equals(job.getCompanyId())) {
                throw new BusinessException(
                        ResultCodeEnum.FORBIDDEN,
                        "Cannot create application for another company");
            }
        }

        application.setId(null);
        application.setCompanyId(job.getCompanyId());
        application.setResumeId(candidate.getResumeId());
        application.setStatus(ApplicationStatus.PENDING.name());

        if (!StringUtils.hasText(application.getVerificationStatus())) {
            application.setVerificationStatus("NOT_CHECKED");
        }

        application.setApplicationDate(LocalDateTime.now());
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        application.setDeleted(0);

        applicationMapper.insert(application);

        return application;
    }

    @Override
    public JobApplication updateApplication(JobApplication application) {
        JobApplication existing = getApplicationById(application.getId());
        checkCompanyAccess(existing);

        existing.setHrNotes(application.getHrNotes());
        existing.setVerificationStatus(application.getVerificationStatus());
        existing.setMismatchScore(application.getMismatchScore());
        existing.setMismatchSummary(application.getMismatchSummary());
        existing.setUpdateTime(LocalDateTime.now());

        applicationMapper.updateById(existing);

        return applicationMapper.selectById(existing.getId());
    }

    @Override
    public void deleteApplication(Long id) {
        JobApplication app = getApplicationById(id);
        checkCompanyAccess(app);
        applicationMapper.deleteById(id);
    }

    @Override
    public JobApplication getApplicationById(Long id) {
        JobApplication app = applicationMapper.selectById(id);

        if (app == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Application not found");
        }

        checkCompanyAccess(app);

        return app;
    }

    @Override
    public Page<JobApplication> pageApplications(
            int page,
            int size,
            Long jobId,
            Long candidateId,
            String status) {

        Page<JobApplication> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();

        if (!CurrentUserUtil.isPlatformAdmin()) {
            Long companyId = CurrentUserUtil.getCurrentCompanyId();

            if (companyId == null) {
                throw new BusinessException(
                        ResultCodeEnum.FORBIDDEN,
                        "Current user has no company scope");
            }

            wrapper.eq(JobApplication::getCompanyId, companyId);
        }

        if (jobId != null) {
            wrapper.eq(JobApplication::getJobId, jobId);
        }

        if (candidateId != null) {
            wrapper.eq(JobApplication::getCandidateId, candidateId);
        }

        if (StringUtils.hasText(status)) {
            wrapper.eq(JobApplication::getStatus, status);
        }

        wrapper.orderByDesc(JobApplication::getApplicationDate);

        return applicationMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public List<JobApplication> listByJobId(Long jobId) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(JobApplication::getJobId, jobId);

        if (!CurrentUserUtil.isPlatformAdmin()) {
            Long companyId = CurrentUserUtil.getCurrentCompanyId();

            if (companyId == null) {
                throw new BusinessException(
                        ResultCodeEnum.FORBIDDEN,
                        "Current user has no company scope");
            }

            wrapper.eq(JobApplication::getCompanyId, companyId);
        }

        wrapper.orderByDesc(JobApplication::getApplicationDate);

        return applicationMapper.selectList(wrapper);
    }

    @Override
    public List<JobApplication> listByCandidateId(Long candidateId) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(JobApplication::getCandidateId, candidateId);

        if (!CurrentUserUtil.isPlatformAdmin()) {
            Long companyId = CurrentUserUtil.getCurrentCompanyId();

            if (companyId == null) {
                throw new BusinessException(
                        ResultCodeEnum.FORBIDDEN,
                        "Current user has no company scope");
            }

            wrapper.eq(JobApplication::getCompanyId, companyId);
        }

        wrapper.orderByDesc(JobApplication::getApplicationDate);

        return applicationMapper.selectList(wrapper);
    }

    @Override
    public boolean changeStatus(Long id, String newStatus, String hrNotes) {
        JobApplication app = getApplicationById(id);
        checkCompanyAccess(app);

        ApplicationStatusTransitionValidator.validate(app.getStatus(), newStatus);

        app.setStatus(newStatus);

        if (hrNotes != null) {
            app.setHrNotes(hrNotes);
        }

        app.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(app);

        return true;
    }

    @Override
    public Map<String, Object> getApplicationDetails(Long id) {
        JobApplication app = getApplicationById(id);
        checkCompanyAccess(app);

        Map<String, Object> details = new HashMap<>();

        details.put("application", app);

        Job job = jobMapper.selectById(app.getJobId());
        details.put("job", job);

        Candidate candidate = candidateMapper.selectById(app.getCandidateId());
        details.put("candidate", candidate);

        return details;
    }

    private void checkCompanyAccess(JobApplication app) {
        if (CurrentUserUtil.isPlatformAdmin()) {
            return;
        }

        Long currentCompanyId = CurrentUserUtil.getCurrentCompanyId();

        if (currentCompanyId == null
                || app.getCompanyId() == null
                || !currentCompanyId.equals(app.getCompanyId())) {

            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN,
                    "Cannot access application from another company");
        }
    }
}