package com.dacn.ATS.module.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.enums.JobStatus;
import com.dacn.ATS.module.job.mapper.JobMapper;
import com.dacn.ATS.module.job.service.JobService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    @Autowired
    private JobMapper jobMapper;

    @Override
    public Job createJob(Job job, Long hrId) {
        validateRequiredJobFields(job);

        job.setId(null);
        job.setHrId(hrId);
        job.setStatus(JobStatus.DRAFT.name());
        job.setPublishDate(null);
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        job.setDeleted(0);

        jobMapper.insert(job);
        return job;
    }

    @Override
    public Job updateJob(Job job) {
        Job existing = getJobById(job.getId());
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Job not found");
        }

        if (JobStatus.CLOSED.name().equals(existing.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Cannot update a closed job");
        }

        validateRequiredJobFields(job);

        job.setStatus(existing.getStatus());
        job.setHrId(existing.getHrId());
        job.setPublishDate(existing.getPublishDate());
        job.setCreateTime(existing.getCreateTime());
        job.setUpdateTime(LocalDateTime.now());

        jobMapper.updateById(job);
        return jobMapper.selectById(job.getId());
    }

    @Override
    public void deleteJob(Long id) {
        Job job = getJobById(id);
        if (job == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Job not found");
        }

        if (JobStatus.PUBLISHED.name().equals(job.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Cannot delete a published job. Please close it first");
        }

        jobMapper.deleteById(id);
    }

    @Override
    public Job getJobById(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public Page<Job> pageJobs(int page, int size, String keyword) {
        Page<Job> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Job::getTitle, keyword)
                    .or()
                    .like(Job::getDepartment, keyword)
                    .or()
                    .like(Job::getLocation, keyword));
        }

        wrapper.orderByDesc(Job::getCreateTime);
        return jobMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public Page<Job> pagePublishedJobs(int page, int size, String keyword) {
        Page<Job> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getStatus, JobStatus.PUBLISHED.name());

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Job::getTitle, keyword)
                    .or()
                    .like(Job::getDepartment, keyword)
                    .or()
                    .like(Job::getLocation, keyword));
        }

        wrapper.orderByDesc(Job::getCreateTime);
        return jobMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public List<Job> listJobsByHrId(Long hrId) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getHrId, hrId);
        wrapper.orderByDesc(Job::getCreateTime);
        return jobMapper.selectList(wrapper);
    }

    @Override
    public boolean changeStatus(Long id, String status, Long currentUserId, String currentUserRole) {
        Job job = getJobById(id);
        if (job == null) {
            return false;
        }

        String normalizedRole = normalizeRole(currentUserRole);
        if (!"ADMIN".equals(normalizedRole) && !job.getHrId().equals(currentUserId)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "You are not the owner of this job");
        }

        JobStatus currentStatus = JobStatus.parse(job.getStatus());
        JobStatus nextStatus = JobStatus.parse(status);

        if (!currentStatus.canTransitionTo(nextStatus)) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "Invalid job status transition: " + currentStatus + " -> " + nextStatus
            );
        }

        if (nextStatus == JobStatus.PUBLISHED) {
            validateRequiredJobFields(job);
            if (job.getPublishDate() == null) {
                job.setPublishDate(LocalDateTime.now());
            }
        }

        job.setStatus(nextStatus.name());
        job.setUpdateTime(LocalDateTime.now());
        jobMapper.updateById(job);

        return true;
    }

    private void validateRequiredJobFields(Job job) {
        if (!StringUtils.hasText(job.getTitle())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Job title is required");
        }
        if (!StringUtils.hasText(job.getDescription())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Job description is required");
        }
        if (!StringUtils.hasText(job.getDepartment())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Department is required");
        }
        if (!StringUtils.hasText(job.getLocation())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Job location is required");
        }
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "";
        }
        return role.replace("ROLE_", "").trim().toUpperCase();
    }
}