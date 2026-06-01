package com.dacn.ATS.module.job.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.job.entity.Job;

import java.util.List;

public interface JobService {
    Job createJob(Job job, Long hrId);

    Job updateJob(Job job);

    void deleteJob(Long id);

    Job getJobById(Long id);

    Page<Job> pageJobs(int page, int size, String keyword);

    List<Job> listJobsByHrId(Long hrId);

    List<Job> listPublishedPublicJobs();

    Job getPublicPublishedJobById(Long id);

    boolean changeStatus(Long id, String status, Long currentUserId, String currentUserRole);
}