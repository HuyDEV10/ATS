package com.dacn.ATS.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.module.ai.entity.ApplicationScore;
import com.dacn.ATS.module.ai.mapper.ApplicationScoreMapper;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.enums.ApplicationStatus;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.dashboard.dto.AiScoreDistributionDTO;
import com.dacn.ATS.module.dashboard.dto.DashboardStatsDTO;
import com.dacn.ATS.module.dashboard.dto.RecruitmentFunnelDTO;
import com.dacn.ATS.module.dashboard.service.DashboardService;
import com.dacn.ATS.module.interview.entity.InterviewRecord;
import com.dacn.ATS.module.interview.mapper.InterviewRecordMapper;
import com.dacn.ATS.module.job.entity.Job;
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

        Long companyId = currentCompanyScope();

        stats.setTotalJobs(jobMapper.selectCount(jobWrapper(companyId)));
        stats.setTotalCandidates(candidateMapper.selectCount(candidateWrapper(companyId)));
        stats.setTotalApplications(applicationMapper.selectCount(applicationWrapper(companyId)));
        stats.setTotalInterviews(interviewMapper.selectCount(interviewWrapper(companyId)));

        LambdaQueryWrapper<JobApplication> pendingWrapper = applicationWrapper(companyId);
        pendingWrapper.eq(JobApplication::getStatus, ApplicationStatus.PENDING.name());
        stats.setPendingApplications(applicationMapper.selectCount(pendingWrapper));

        LambdaQueryWrapper<InterviewRecord> scheduledWrapper = interviewWrapper(companyId);
        scheduledWrapper.eq(InterviewRecord::getStatus, "SCHEDULED");
        stats.setScheduledInterviews(interviewMapper.selectCount(scheduledWrapper));

        return stats;
    }

    @Override
    public RecruitmentFunnelDTO getRecruitmentFunnel() {
        Long companyId = currentCompanyScope();

        RecruitmentFunnelDTO funnel = new RecruitmentFunnelDTO();

        funnel.setPending(countApplications(companyId, ApplicationStatus.PENDING.name()));
        funnel.setAiScreened(countApplications(companyId, ApplicationStatus.AI_SCREENED.name()));
        funnel.setShortlisted(countApplications(companyId, ApplicationStatus.SHORTLISTED.name()));
        funnel.setInterviewScheduled(countApplications(companyId, ApplicationStatus.INTERVIEW_SCHEDULED.name()));
        funnel.setInterviewed(countApplications(companyId, ApplicationStatus.INTERVIEWED.name()));
        funnel.setOffered(countApplications(companyId, ApplicationStatus.OFFERED.name()));
        funnel.setRejected(countApplications(companyId, ApplicationStatus.REJECTED.name()));

        return funnel;
    }

    @Override
    public AiScoreDistributionDTO getAiScoreDistribution() {
        Long companyId = currentCompanyScope();

        AiScoreDistributionDTO distribution = new AiScoreDistributionDTO();

        distribution.setStrongMatches(countScores(companyId, 80, 101));
        distribution.setPotentialMatches(countScores(companyId, 60, 80));
        distribution.setWeakMatches(countScores(companyId, 40, 60));
        distribution.setNotMatches(countScores(companyId, 0, 40));

        return distribution;
    }

    private long countApplications(Long companyId, String status) {
        LambdaQueryWrapper<JobApplication> wrapper = applicationWrapper(companyId);
        wrapper.eq(JobApplication::getStatus, status);
        return applicationMapper.selectCount(wrapper);
    }

    private long countScores(Long companyId, int minInclusive, int maxExclusive) {
        LambdaQueryWrapper<ApplicationScore> wrapper = scoreWrapper(companyId);

        wrapper.ge(ApplicationScore::getOverallScore, minInclusive)
                .lt(ApplicationScore::getOverallScore, maxExclusive);

        return scoreMapper.selectCount(wrapper);
    }

    /**
     * Platform Admin xem toàn hệ thống.
     * Company Owner/HR/Interviewer/Viewer chỉ xem company hiện tại.
     */
    private Long currentCompanyScope() {
        if (CurrentUserUtil.isPlatformAdmin()) {
            return null;
        }

        return CurrentUserUtil.getCurrentCompanyId();
    }

    private LambdaQueryWrapper<Job> jobWrapper(Long companyId) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();

        if (companyId != null) {
            wrapper.eq(Job::getCompanyId, companyId);
        }

        return wrapper;
    }

    private LambdaQueryWrapper<Candidate> candidateWrapper(Long companyId) {
        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();

        if (companyId != null) {
            wrapper.eq(Candidate::getCompanyId, companyId);
        }

        return wrapper;
    }

    private LambdaQueryWrapper<JobApplication> applicationWrapper(Long companyId) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();

        if (companyId != null) {
            wrapper.eq(JobApplication::getCompanyId, companyId);
        }

        return wrapper;
    }

    private LambdaQueryWrapper<InterviewRecord> interviewWrapper(Long companyId) {
        LambdaQueryWrapper<InterviewRecord> wrapper = new LambdaQueryWrapper<>();

        if (companyId != null) {
            wrapper.eq(InterviewRecord::getCompanyId, companyId);
        }

        return wrapper;
    }

    private LambdaQueryWrapper<ApplicationScore> scoreWrapper(Long companyId) {
        LambdaQueryWrapper<ApplicationScore> wrapper = new LambdaQueryWrapper<>();

        if (companyId != null) {
            wrapper.eq(ApplicationScore::getCompanyId, companyId);
        }

        return wrapper;
    }
}