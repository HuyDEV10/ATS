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
import com.dacn.ATS.module.job.mapper.JobMapper;

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

        application.setId(null);
        application.setCompanyId(job.getCompanyId());
        application.setStatus(ApplicationStatus.PENDING.name());
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
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Application not found");
        }

        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);
        return applicationMapper.selectById(application.getId());
    }

    @Override
    public void deleteApplication(Long id) {
        JobApplication app = getApplicationById(id);
        if (app == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
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
            wrapper.eq(JobApplication::getStatus, status);
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
        if (app == null) {
            return null;
        }

        Map<String, Object> details = new HashMap<>();

        details.put("application", app);

        Job job = jobMapper.selectById(app.getJobId());
        details.put("job", job);

        Candidate candidate = candidateMapper.selectById(app.getCandidateId());
        details.put("candidate", candidate);

        return details;
    }
}