package com.dacn.ATS.module.verification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.verification.dto.SkillEvidenceRequest;
import com.dacn.ATS.module.verification.dto.SkillMatchResult;
import com.dacn.ATS.module.verification.dto.VerifiedSkillProfile;
import com.dacn.ATS.module.verification.entity.SkillVerification;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VerificationService {
    SkillVerification submit(SkillVerification verification);

    SkillVerification submitEvidence(SkillEvidenceRequest request, MultipartFile certificateFile);

    SkillVerification markVerified(Long id, String reviewerNote);

    SkillVerification reject(Long id, String reviewerNote);

    Page<SkillVerification> pageByCandidate(Long candidateId, int page, int size);

    List<SkillVerification> listByCandidate(Long candidateId);

    VerifiedSkillProfile buildVerifiedSkillProfile(Long candidateId);

    SkillMatchResult compareCandidateWithJob(Long candidateId, Long jobId);
}