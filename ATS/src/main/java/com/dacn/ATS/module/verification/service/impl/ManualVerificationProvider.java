package com.dacn.ATS.module.verification.service.impl;

import com.dacn.ATS.module.verification.entity.SkillVerification;
import com.dacn.ATS.module.verification.service.VerificationProvider;
import org.springframework.stereotype.Component;

@Component
public class ManualVerificationProvider implements VerificationProvider {
    @Override
    public boolean supports(String provider) {
        return provider == null || provider.isBlank() || "MANUAL".equalsIgnoreCase(provider);
    }

    @Override
    public boolean verify(SkillVerification verification) {
        return verification.getEvidenceText() != null && !verification.getEvidenceText().isBlank();
    }
}