package com.dacn.ATS.module.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.job.entity.Job;
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
        Long companyId = CurrentUserUtil.getCurrentCompanyId();

        if (!CurrentUserUtil.isPlatformAdmin() && companyId == null) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "Current user does not belong to a company");
        }

        job.setId(null);
        job.setCompanyId(companyId);
        job.setHrId(hrId);
        job.setStatus("DRAFT");
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
        checkCompanyAccess(existing);

        existing.setTitle(job.getTitle());
        existing.setDescription(job.getDescription());
        existing.setDepartment(job.getDepartment());
        existing.setLocation(job.getLocation());
        existing.setSalaryRange(job.getSalaryRange());
        existing.setUpdateTime(LocalDateTime.now());

        jobMapper.updateById(existing);
        return jobMapper.selectById(existing.getId());
    }

    @Override
    public void deleteJob(Long id) {
        Job job = getJobById(id);
        checkCompanyAccess(job);
        jobMapper.deleteById(id);
    }

    @Override
    public Job getJobById(Long id) {
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Job not found");
        }
        checkCompanyAccess(job);
        return job;
    }

    @Override
    public Page<Job> pageJobs(int page, int size, String keyword) {
        Page<Job> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();

        Long companyId = CurrentUserUtil.getCurrentCompanyId();
        if (!CurrentUserUtil.isPlatformAdmin()) {
            wrapper.eq(Job::getCompanyId, companyId);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Job::getTitle, keyword)
                    .or()
                    .like(Job::getDepartment, keyword));
        }

        wrapper.orderByDesc(Job::getCreateTime);
        return jobMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public List<Job> listJobsByHrId(Long hrId) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getHrId, hrId);

        Long companyId = CurrentUserUtil.getCurrentCompanyId();
        if (!CurrentUserUtil.isPlatformAdmin()) {
            wrapper.eq(Job::getCompanyId, companyId);
        }

        wrapper.orderByDesc(Job::getCreateTime);
        return jobMapper.selectList(wrapper);
    }

    @Override
    public boolean changeStatus(Long id, String status, Long currentUserId, String currentUserRole) {
        Job job = jobMapper.selectById(id);
        if (job == null) {
            return false;
        }

        checkCompanyAccess(job);

        if (!CurrentUserUtil.isPlatformAdmin()
                && !"COMPANY_OWNER".equals(currentUserRole)
                && !job.getHrId().equals(currentUserId)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "You are not allowed to change this job status");
        }

        job.setStatus(status);

        if ("PUBLISHED".equals(status) && job.getPublishDate() == null) {
            job.setPublishDate(LocalDateTime.now());
        }

        job.setUpdateTime(LocalDateTime.now());
        jobMapper.updateById(job);
        return true;
    }

    private void checkCompanyAccess(Job job) {
        if (CurrentUserUtil.isPlatformAdmin()) {
            return;
        }

        Long currentCompanyId = CurrentUserUtil.getCurrentCompanyId();

        if (currentCompanyId == null || job.getCompanyId() == null || !currentCompanyId.equals(job.getCompanyId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "Cannot access another company's job");
        }
    }
}
