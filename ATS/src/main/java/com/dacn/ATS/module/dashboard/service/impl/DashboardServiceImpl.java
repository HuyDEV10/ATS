package com.dacn.ATS.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dacn.ATS.module.ai.entity.ApplicationScore;
import com.dacn.ATS.module.ai.mapper.ApplicationScoreMapper;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.enums.ApplicationStatus;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.dashboard.dto.AiScoreDistributionDTO;
import com.dacn.ATS.module.dashboard.dto.DashboardStatsDTO;
import com.dacn.ATS.module.dashboard.dto.RecruitmentFunnelDTO;
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
    private final ApplicationScoreMapper scoreMapper;

    public DashboardServiceImpl(
            JobMapper jobMapper,
            CandidateMapper candidateMapper,
            JobApplicationMapper applicationMapper,
            InterviewRecordMapper interviewMapper,
            ApplicationScoreMapper scoreMapper) {
        this.jobMapper = jobMapper;
        this.candidateMapper = candidateMapper;
        this.applicationMapper = applicationMapper;
        this.interviewMapper = interviewMapper;
        this.scoreMapper = scoreMapper;
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

    @Override
    public RecruitmentFunnelDTO getRecruitmentFunnel() {
        RecruitmentFunnelDTO funnel = new RecruitmentFunnelDTO();
        funnel.setPending(countApplications(ApplicationStatus.PENDING.name()));
        funnel.setAiScreened(countApplications(ApplicationStatus.AI_SCREENED.name()));
        funnel.setShortlisted(countApplications(ApplicationStatus.SHORTLISTED.name()));
        funnel.setInterviewScheduled(countApplications(ApplicationStatus.INTERVIEW_SCHEDULED.name()));
        funnel.setInterviewed(countApplications(ApplicationStatus.INTERVIEWED.name()));
        funnel.setOffered(countApplications(ApplicationStatus.OFFERED.name()));
        funnel.setRejected(countApplications(ApplicationStatus.REJECTED.name()));
        return funnel;
    }

    @Override
    public AiScoreDistributionDTO getAiScoreDistribution() {
        AiScoreDistributionDTO distribution = new AiScoreDistributionDTO();
        distribution.setStrongMatches(countScores(80, 101));
        distribution.setPotentialMatches(countScores(60, 80));
        distribution.setWeakMatches(countScores(40, 60));
        distribution.setNotMatches(countScores(0, 40));
        return distribution;
    }

    private long countApplications(String status) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobApplication::getStatus, status);
        return applicationMapper.selectCount(wrapper);
    }

    private long countScores(int minInclusive, int maxExclusive) {
        LambdaQueryWrapper<ApplicationScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(ApplicationScore::getOverallScore, minInclusive)
                .lt(ApplicationScore::getOverallScore, maxExclusive);
        return scoreMapper.selectCount(wrapper);
    }
}