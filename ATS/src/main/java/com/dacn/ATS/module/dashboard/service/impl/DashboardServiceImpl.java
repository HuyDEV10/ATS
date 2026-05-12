package com.dacn.ATS.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.dashboard.dto.DashboardStatsDTO;
import com.dacn.ATS.module.dashboard.service.DashboardService;
import com.dacn.ATS.module.interview.entity.InterviewRecord;
import com.dacn.ATS.module.interview.mapper.InterviewRecordMapper;
import com.dacn.ATS.module.job.mapper.JobMapper;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final JobMapper jobMapper;
    private final CandidateMapper candidateMapper;
    private final JobApplicationMapper applicationMapper;
    private final InterviewRecordMapper interviewMapper;

    public DashboardServiceImpl(
            JobMapper jobMapper,
            CandidateMapper candidateMapper,
            JobApplicationMapper applicationMapper,
            InterviewRecordMapper interviewMapper) {
        this.jobMapper = jobMapper;
        this.candidateMapper = candidateMapper;
        this.applicationMapper = applicationMapper;
        this.interviewMapper = interviewMapper;
    }

    @Override
    public DashboardStatsDTO getStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        stats.setTotalJobs(jobMapper.selectCount(null));
        stats.setTotalCandidates(candidateMapper.selectCount(null));
        stats.setTotalApplications(applicationMapper.selectCount(null));
        stats.setTotalInterviews(interviewMapper.selectCount(null));

        LambdaQueryWrapper<JobApplication> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(JobApplication::getStatus, "PENDING");
        stats.setPendingApplications(applicationMapper.selectCount(pendingWrapper));

        LambdaQueryWrapper<InterviewRecord> scheduledWrapper = new LambdaQueryWrapper<>();
        scheduledWrapper.eq(InterviewRecord::getStatus, "SCHEDULED");
        stats.setScheduledInterviews(interviewMapper.selectCount(scheduledWrapper));

        return stats;
    }
}