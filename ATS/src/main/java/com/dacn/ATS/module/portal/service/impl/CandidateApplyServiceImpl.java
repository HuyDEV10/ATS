package com.dacn.ATS.module.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
import com.dacn.ATS.module.application.service.JobApplicationService;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.candidate.service.CandidateService;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.mapper.JobMapper;
import com.dacn.ATS.module.portal.service.CandidateApplyService;
import com.dacn.ATS.module.resume.entity.Resume;
import com.dacn.ATS.module.resume.service.ResumeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CandidateApplyServiceImpl implements CandidateApplyService {

    private final ResumeService resumeService;
    private final CandidateService candidateService;
    private final JobApplicationService applicationService;
    private final JobMapper jobMapper;
    private final CandidateMapper candidateMapper;
    private final JobApplicationMapper applicationMapper;

    public CandidateApplyServiceImpl(
            ResumeService resumeService,
            CandidateService candidateService,
            JobApplicationService applicationService,
            JobMapper jobMapper,
            CandidateMapper candidateMapper,
            JobApplicationMapper applicationMapper) {
        this.resumeService = resumeService;
        this.candidateService = candidateService;
        this.applicationService = applicationService;
        this.jobMapper = jobMapper;
        this.candidateMapper = candidateMapper;
        this.applicationMapper = applicationMapper;
    }

    @Override
    @Transactional
    public void apply(
            Long jobId,
            String name,
            String email,
            String phone,
            String skills,
            Integer experienceYears,
            MultipartFile file) {
        validatePublicApplication(jobId, email);
        try {
            Resume resume = resumeService.uploadResume(file, 0L);

            Candidate candidate = new Candidate();
            candidate.setName(name);
            candidate.setEmail(email);
            candidate.setPhone(phone);
            candidate.setSkills(skills);
            candidate.setExperienceYears(experienceYears);
            candidate.setResumeId(resume.getId());
            candidate.setSource("public_apply");

            candidateService.createCandidate(candidate, 0L);

            JobApplication application = new JobApplication();
            application.setJobId(jobId);
            application.setCandidateId(candidate.getId());

            applicationService.createApplication(application);

        } catch (Exception e) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Apply failed: " + e.getMessage());
        }
    }

    private void validatePublicApplication(Long jobId, String email) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Job not found");
        }
        if (!"PUBLISHED".equals(job.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Job is not open for public applications");
        }

        LambdaQueryWrapper<Candidate> candidateWrapper = new LambdaQueryWrapper<>();
        candidateWrapper.eq(Candidate::getEmail, email);
        for (Candidate candidate : candidateMapper.selectList(candidateWrapper)) {
            LambdaQueryWrapper<JobApplication> applicationWrapper = new LambdaQueryWrapper<>();
            applicationWrapper.eq(JobApplication::getJobId, jobId)
                    .eq(JobApplication::getCandidateId, candidate.getId());
            if (applicationMapper.selectCount(applicationWrapper) > 0) {
                throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Candidate already applied to this job");
            }
        }
    }
}