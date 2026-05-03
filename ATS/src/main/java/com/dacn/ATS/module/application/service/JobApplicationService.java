package com.dacn.ATS.module.application.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.application.entity.JobApplication;

import java.util.List;
import java.util.Map;

public interface JobApplicationService {
    JobApplication createApplication(JobApplication application);

    JobApplication updateApplication(JobApplication application);

    void deleteApplication(Long id);

    JobApplication getApplicationById(Long id);

    Page<JobApplication> pageApplications(int page, int size, Long jobId, Long candidateId, String status);

    List<JobApplication> listByJobId(Long jobId);

    List<JobApplication> listByCandidateId(Long candidateId);

    boolean changeStatus(Long id, String newStatus, String hrNotes);

    Map<String, Object> getApplicationDetails(Long id); // kèm tên job và candidate
}
