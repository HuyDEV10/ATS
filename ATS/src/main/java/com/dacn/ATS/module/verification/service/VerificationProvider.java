package com.dacn.ATS.module.verification.service;

import com.dacn.ATS.module.verification.entity.SkillVerification;

public interface VerificationProvider {
    boolean supports(String provider);

    boolean verify(SkillVerification verification);
}