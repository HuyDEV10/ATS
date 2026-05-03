package com.dacn.ATS.module.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
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
        job.setId(null);
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
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Job not found");
        }
        // Chỉ cho phép update nếu status != CLOSED (tuỳ logic)
        job.setUpdateTime(LocalDateTime.now());
        jobMapper.updateById(job);
        return jobMapper.selectById(job.getId());
    }

    @Override
    public void deleteJob(Long id) {
        Job job = getJobById(id);
        if (job == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        // Soft delete (MyBatis-Plus tự động set deleted=1)
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
            wrapper.like(Job::getTitle, keyword)
                    .or()
                    .like(Job::getDepartment, keyword);
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
        if (job == null)
            return false;
        // Kiểm tra quyền: ADMIN hoặc HR của chính job đó
        if (!"ADMIN".equals(currentUserRole) && !job.getHrId().equals(currentUserId)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "You are not the owner of this job");
        }
        job.setStatus(status);
        if ("PUBLISHED".equals(status) && job.getPublishDate() == null) {
            job.setPublishDate(LocalDateTime.now());
        }
        jobMapper.updateById(job);
        return true;
    }
}