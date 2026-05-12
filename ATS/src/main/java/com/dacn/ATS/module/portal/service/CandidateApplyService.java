package com.dacn.ATS.module.portal.service;

import org.springframework.web.multipart.MultipartFile;

public interface CandidateApplyService {
    void apply(Long jobId, String name, String email, String phone, String skills,
            Integer experienceYears, MultipartFile file);
}