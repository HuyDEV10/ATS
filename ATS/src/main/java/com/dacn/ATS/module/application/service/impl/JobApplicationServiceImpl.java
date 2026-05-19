package com.dacn.ATS.module.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.enums.ApplicationStatus;
import com.dacn.ATS.module.application.enums.ApplicationStatusTransitionValidator;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
import com.dacn.ATS.module.application.service.JobApplicationService;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.enums.JobStatus;
import com.dacn.ATS.module.job.mapper.JobMapper;
import com.dacn.ATS.module.notification.service.CandidateEmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobApplicationServiceImpl implements JobApplicationService {

    @Autowired
    private JobApplicationMapper applicationMapper;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private CandidateMapper candidateMapper;

    @Autowired
    private CandidateEmailService candidateEmailService;

    @Override
    public JobApplication createApplication(JobApplication application) {
        Job job = jobMapper.selectById(application.getJobId());
        if (job == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Job not found");
        }

        if (!JobStatus.PUBLISHED.name().equals(job.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Only published jobs can receive applications");
        }

        Candidate candidate = candidateMapper.selectById(application.getCandidateId());
        if (candidate == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Candidate not found");
        }

        checkDuplicateApplication(application.getJobId(), application.getCandidateId());

        application.setId(null);
        application.setStatus(ApplicationStatus.PENDING.name());
        application.setApplicationDate(LocalDateTime.now());
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        application.setDeleted(0);

        applicationMapper.insert(application);

        candidateEmailService.sendApplicationCreatedEmail(candidate, job, application);

        return application;
    }

    @Override
    public JobApplication updateApplication(JobApplication application) {
        JobApplication existing = getApplicationById(application.getId());
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Application not found");
        }

        Job job = jobMapper.selectById(application.getJobId());
        if (job == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Job not found");
        }

        Candidate candidate = candidateMapper.selectById(application.getCandidateId());
        if (candidate == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Candidate not found");
        }

        application.setStatus(existing.getStatus());
        application.setApplicationDate(existing.getApplicationDate());
        application.setCreateTime(existing.getCreateTime());
        application.setUpdateTime(LocalDateTime.now());

        applicationMapper.updateById(application);
        return applicationMapper.selectById(application.getId());
    }

    @Override
    public void deleteApplication(Long id) {
        JobApplication app = getApplicationById(id);
        if (app == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Application not found");
        }

        if (!ApplicationStatus.PENDING.name().equals(app.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Only pending applications can be deleted");
        }

        applicationMapper.deleteById(id);
    }

    @Override
    public JobApplication getApplicationById(Long id) {
        return applicationMapper.selectById(id);
    }

    @Override
    public Page<JobApplication> pageApplications(int page, int size, Long jobId, Long candidateId, String status) {
        Page<JobApplication> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();

        if (jobId != null) {
            wrapper.eq(JobApplication::getJobId, jobId);
        }

        if (candidateId != null) {
            wrapper.eq(JobApplication::getCandidateId, candidateId);
        }

        if (StringUtils.hasText(status)) {
            String normalizedStatus = status.trim().toUpperCase();
            ApplicationStatusTransitionValidator.parse(normalizedStatus);
            wrapper.eq(JobApplication::getStatus, normalizedStatus);
        }

        wrapper.orderByDesc(JobApplication::getApplicationDate);
        return applicationMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public List<JobApplication> listByJobId(Long jobId) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobApplication::getJobId, jobId);
        wrapper.orderByDesc(JobApplication::getApplicationDate);
        return applicationMapper.selectList(wrapper);
    }

    @Override
    public List<JobApplication> listByCandidateId(Long candidateId) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobApplication::getCandidateId, candidateId);
        wrapper.orderByDesc(JobApplication::getApplicationDate);
        return applicationMapper.selectList(wrapper);
    }

    @Override
    public boolean changeStatus(Long id, String newStatus, String hrNotes) {
        JobApplication app = getApplicationById(id);
        if (app == null) {
            return false;
        }

        String oldStatus = app.getStatus();
        String normalizedStatus = newStatus.trim().toUpperCase();

        ApplicationStatusTransitionValidator.validate(oldStatus, normalizedStatus);

        validateStatusNote(normalizedStatus, hrNotes);

        app.setStatus(normalizedStatus);

        if (hrNotes != null) {
            app.setHrNotes(hrNotes);
        }

        app.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(app);

        Job job = jobMapper.selectById(app.getJobId());
        Candidate candidate = candidateMapper.selectById(app.getCandidateId());

        candidateEmailService.sendApplicationStatusChangedEmail(
                candidate,
                job,
                app,
                oldStatus,
                normalizedStatus,
                hrNotes);

        return true;
    }

    @Override
    public Map<String, Object> getApplicationDetails(Long id) {
        JobApplication app = getApplicationById(id);
        if (app == null) {
            return null;
        }

        Map<String, Object> details = new HashMap<>();
        details.put("application", app);
        details.put("job", jobMapper.selectById(app.getJobId()));
        details.put("candidate", candidateMapper.selectById(app.getCandidateId()));

        return details;
    }

    private void checkDuplicateApplication(Long jobId, Long candidateId) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobApplication::getJobId, jobId)
                .eq(JobApplication::getCandidateId, candidateId);

        Long count = applicationMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Candidate has already applied for this job");
        }
    }

    private void validateStatusNote(String newStatus, String hrNotes) {
        if (ApplicationStatus.REJECTED.name().equals(newStatus) && !StringUtils.hasText(hrNotes)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Reject reason is required");
        }

        if (ApplicationStatus.OFFERED.name().equals(newStatus) && !StringUtils.hasText(hrNotes)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Offer note is required");
        }
    }
}