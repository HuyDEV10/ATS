package com.dacn.ATS.module.verification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.verification.entity.SkillVerification;
import com.dacn.ATS.module.verification.service.VerificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verifications")
public class VerificationController {
    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping
    public SkillVerification submit(@RequestBody SkillVerification verification) {
        return verificationService.submit(verification);
    }

    @PostMapping("/{id}/verify")
    public SkillVerification verify(@PathVariable Long id, @RequestParam(required = false) String note) {
        return verificationService.markVerified(id, note);
    }

    @PostMapping("/{id}/reject")
    public SkillVerification reject(@PathVariable Long id, @RequestParam(required = false) String note) {
        return verificationService.reject(id, note);
    }

    @GetMapping("/candidate/{candidateId}")
    public Page<SkillVerification> listByCandidate(
            @PathVariable Long candidateId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return verificationService.pageByCandidate(candidateId, page, size);
    }
}