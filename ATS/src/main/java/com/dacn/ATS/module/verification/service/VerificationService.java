package com.dacn.ATS.module.verification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.verification.entity.SkillVerification;

public interface VerificationService {
    SkillVerification submit(SkillVerification verification);

    SkillVerification markVerified(Long id, String reviewerNote);

    SkillVerification reject(Long id, String reviewerNote);

    Page<SkillVerification> pageByCandidate(Long candidateId, int page, int size);
}