package com.dacn.ATS.module.verification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.verification.entity.SkillVerification;
import com.dacn.ATS.module.verification.enums.VerificationStatus;
import com.dacn.ATS.module.verification.mapper.SkillVerificationMapper;
import com.dacn.ATS.module.verification.service.VerificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VerificationServiceImpl implements VerificationService {
    private final SkillVerificationMapper verificationMapper;

    public VerificationServiceImpl(SkillVerificationMapper verificationMapper) {
        this.verificationMapper = verificationMapper;
    }

    @Override
    public SkillVerification submit(SkillVerification verification) {
        verification.setId(null);
        verification.setStatus(VerificationStatus.SUBMITTED.name());
        verification.setCreateTime(LocalDateTime.now());
        verification.setUpdateTime(LocalDateTime.now());
        verification.setDeleted(0);
        verificationMapper.insert(verification);
        return verification;
    }

    @Override
    public SkillVerification markVerified(Long id, String reviewerNote) {
        SkillVerification verification = getRequired(id);
        verification.setStatus(VerificationStatus.VERIFIED.name());
        verification.setEvidenceText(appendReviewerNote(verification.getEvidenceText(), reviewerNote));
        verification.setVerifiedAt(LocalDateTime.now());
        verification.setUpdateTime(LocalDateTime.now());
        verificationMapper.updateById(verification);
        return verification;
    }

    @Override
    public SkillVerification reject(Long id, String reviewerNote) {
        SkillVerification verification = getRequired(id);
        verification.setStatus(VerificationStatus.REJECTED.name());
        verification.setEvidenceText(appendReviewerNote(verification.getEvidenceText(), reviewerNote));
        verification.setUpdateTime(LocalDateTime.now());
        verificationMapper.updateById(verification);
        return verification;
    }

    @Override
    public Page<SkillVerification> pageByCandidate(Long candidateId, int page, int size) {
        LambdaQueryWrapper<SkillVerification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillVerification::getCandidateId, candidateId)
                .orderByDesc(SkillVerification::getCreateTime);
        return verificationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private SkillVerification getRequired(Long id) {
        SkillVerification verification = verificationMapper.selectById(id);
        if (verification == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Skill verification not found");
        }
        return verification;
    }

    private String appendReviewerNote(String current, String reviewerNote) {
        if (reviewerNote == null || reviewerNote.isBlank()) {
            return current;
        }
        String prefix = current == null || current.isBlank() ? "" : current + "\n";
        return prefix + "Reviewer note: " + reviewerNote;
    }
}