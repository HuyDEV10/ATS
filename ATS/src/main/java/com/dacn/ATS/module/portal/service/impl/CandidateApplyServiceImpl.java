package com.dacn.ATS.module.portal.service.impl;

import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.service.JobApplicationService;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.service.CandidateService;
import com.dacn.ATS.module.portal.service.CandidateApplyService;
import com.dacn.ATS.module.resume.entity.Resume;
import com.dacn.ATS.module.resume.service.ResumeService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CandidateApplyServiceImpl implements CandidateApplyService {

    private final ResumeService resumeService;
    private final CandidateService candidateService;
    private final JobApplicationService applicationService;

    public CandidateApplyServiceImpl(
            ResumeService resumeService,
            CandidateService candidateService,
            JobApplicationService applicationService) {
        this.resumeService = resumeService;
        this.candidateService = candidateService;
        this.applicationService = applicationService;
    }

    @Override
    public void apply(
            Long jobId,
            String name,
            String email,
            String phone,
            String skills,
            Integer experienceYears,
            MultipartFile file) {
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
            throw new RuntimeException("Apply failed: " + e.getMessage());
        }
    }
}