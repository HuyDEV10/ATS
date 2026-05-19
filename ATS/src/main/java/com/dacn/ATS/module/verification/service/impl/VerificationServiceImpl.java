package com.dacn.ATS.module.verification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.ai.service.SkillExtractionService;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.mapper.JobMapper;
import com.dacn.ATS.module.verification.dto.SkillEvidenceRequest;
import com.dacn.ATS.module.verification.dto.SkillMatchResult;
import com.dacn.ATS.module.verification.dto.VerifiedSkillItem;
import com.dacn.ATS.module.verification.dto.VerifiedSkillProfile;
import com.dacn.ATS.module.verification.entity.SkillVerification;
import com.dacn.ATS.module.verification.enums.VerificationSourceType;
import com.dacn.ATS.module.verification.enums.VerificationStatus;
import com.dacn.ATS.module.verification.mapper.SkillVerificationMapper;
import com.dacn.ATS.module.verification.service.VerificationService;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VerificationServiceImpl implements VerificationService {
    private static final int DEFAULT_PAGE_SIZE_LIMIT = 100;

    private final SkillVerificationMapper verificationMapper;
    private final CandidateMapper candidateMapper;
    private final JobMapper jobMapper;
    private final SkillExtractionService skillExtractionService;
    private final Tika tika = new Tika();

    @Value("${resume.upload-dir:./uploads}")
    private String uploadDir;

    public VerificationServiceImpl(
            SkillVerificationMapper verificationMapper,
            CandidateMapper candidateMapper,
            JobMapper jobMapper,
            SkillExtractionService skillExtractionService) {
        this.verificationMapper = verificationMapper;
        this.candidateMapper = candidateMapper;
        this.jobMapper = jobMapper;
        this.skillExtractionService = skillExtractionService;
    }

    @Override
    public SkillVerification submit(SkillVerification verification) {
        normalizeLegacyPayload(verification);
        analyzeAndScore(verification);
        verification.setId(null);
        verification.setStatus(resolveInitialStatus(verification));
        verification.setCreateTime(LocalDateTime.now());
        verification.setUpdateTime(LocalDateTime.now());
        verification.setDeleted(0);
        verificationMapper.insert(verification);
        return verification;
    }

    @Override
    public SkillVerification submitEvidence(SkillEvidenceRequest request, MultipartFile certificateFile) {
        if (request == null || request.getCandidateId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Candidate is required for skill verification");
        }
        if (candidateMapper.selectById(request.getCandidateId()) == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Candidate not found");
        }

        SkillVerification verification = new SkillVerification();
        verification.setCandidateId(request.getCandidateId());
        verification.setSkillName(trimToNull(request.getDeclaredSkill()));
        verification.setSourceType(resolveSourceType(request.getSourceType()).name());
        verification.setProvider(resolveProvider(request));
        verification.setSourceName(trimToNull(request.getSourceName()));
        verification.setCertificateUrl(trimToNull(request.getSourceUrl()));
        verification.setSourceUrl(trimToNull(request.getSourceUrl()));
        verification.setEvidenceText(trimToNull(request.getEvidenceText()));

        if (certificateFile != null && !certificateFile.isEmpty()) {
            StoredArtifact artifact = storeCertificateArtifact(request.getCandidateId(), certificateFile);
            verification.setArtifactPath(artifact.path());
            verification.setEvidenceText(joinEvidence(verification.getEvidenceText(), artifact.extractedText()));
            if (!StringUtils.hasText(verification.getSourceName())) {
                verification.setSourceName(certificateFile.getOriginalFilename());
            }
            if (!VerificationSourceType.CERTIFICATE_LINK.name().equals(verification.getSourceType())) {
                verification.setSourceType(VerificationSourceType.CERTIFICATE_UPLOAD.name());
            }
        }

        return submit(verification);
    }

    @Override
    public SkillVerification markVerified(Long id, String reviewerNote) {
        SkillVerification verification = getRequired(id);
        verification.setStatus(VerificationStatus.VERIFIED.name());
        verification.setEvidenceText(appendReviewerNote(verification.getEvidenceText(), reviewerNote));
        verification.setConfidenceScore(Math.max(nullToZero(verification.getConfidenceScore()), 85));
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
        verification.setConfidenceScore(Math.min(nullToZero(verification.getConfidenceScore()), 30));
        verification.setUpdateTime(LocalDateTime.now());
        verificationMapper.updateById(verification);
        return verification;
    }

    @Override
    public Page<SkillVerification> pageByCandidate(Long candidateId, int page, int size) {
        LambdaQueryWrapper<SkillVerification> wrapper = candidateWrapper(candidateId);
        int safeSize = Math.min(Math.max(size, 1), DEFAULT_PAGE_SIZE_LIMIT);
        return verificationMapper.selectPage(new Page<>(Math.max(page, 1), safeSize), wrapper);
    }

    @Override
    public List<SkillVerification> listByCandidate(Long candidateId) {
        return verificationMapper.selectList(candidateWrapper(candidateId));
    }

    @Override
    public VerifiedSkillProfile buildVerifiedSkillProfile(Long candidateId) {
        List<SkillVerification> verifications = listByCandidate(candidateId);
        Map<String, List<SkillVerification>> bySkill = new LinkedHashMap<>();
        for (SkillVerification verification : verifications) {
            for (String skill : skillsFromVerification(verification)) {
                bySkill.computeIfAbsent(normalizeSkill(skill), ignored -> new ArrayList<>()).add(verification);
            }
        }

        VerifiedSkillProfile profile = new VerifiedSkillProfile();
        profile.setCandidateId(candidateId);
        profile.setTotalEvidence(verifications.size());
        profile.setVerifiedEvidence((int) verifications.stream()
                .filter(v -> VerificationStatus.VERIFIED.name().equals(v.getStatus()))
                .count());

        List<VerifiedSkillItem> items = bySkill.entrySet().stream()
                .map(entry -> toVerifiedSkillItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(VerifiedSkillItem::getConfidenceScore).reversed())
                .toList();
        profile.setSkills(items);
        profile.setAverageConfidence(items.isEmpty() ? 0
                : (int) Math.round(items.stream()
                        .mapToInt(VerifiedSkillItem::getConfidenceScore)
                        .average()
                        .orElse(0)));
        return profile;
    }

    @Override
    public SkillMatchResult compareCandidateWithJob(Long candidateId, Long jobId) {
        Candidate candidate = candidateMapper.selectById(candidateId);
        Job job = jobMapper.selectById(jobId);
        if (candidate == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Candidate not found");
        }
        if (job == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Job not found");
        }

        Set<String> requiredSkills = skillExtractionService.extractSkills(safe(job.getTitle()) + " "
                + safe(job.getDescription()) + " " + safe(job.getDepartment()));
        Set<String> verifiedSkills = buildVerifiedSkillProfile(candidateId).getSkills().stream()
                .filter(item -> item.getConfidenceScore() >= 50)
                .map(item -> normalizeSkill(item.getSkillName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> declaredSkills = skillExtractionService.extractSkills(safe(candidate.getSkills()));

        List<String> matchedVerified = requiredSkills.stream()
                .filter(verifiedSkills::contains)
                .toList();
        List<String> missing = requiredSkills.stream()
                .filter(skill -> !verifiedSkills.contains(skill) && !declaredSkills.contains(skill))
                .toList();

        int verifiedCoverage = requiredSkills.isEmpty()
                ? 0
                : (int) Math.round(matchedVerified.size() * 100.0 / requiredSkills.size());
        int declaredCoverage = requiredSkills.isEmpty()
                ? 0
                : (int) Math.round(requiredSkills.stream().filter(declaredSkills::contains).count() * 100.0
                        / requiredSkills.size());
        int matchScore = Math.min(100, (int) Math.round(verifiedCoverage * 0.7 + declaredCoverage * 0.3));

        SkillMatchResult result = new SkillMatchResult();
        result.setCandidateId(candidateId);
        result.setJobId(jobId);
        result.setVerifiedCoverageScore(verifiedCoverage);
        result.setMatchScore(matchScore);
        result.setMatchedVerifiedSkills(matchedVerified);
        result.setMissingSkills(missing);
        result.setExplanation(
                "Match Score ưu tiên kỹ năng đã xác thực (70%) và bổ sung kỹ năng khai báo trong hồ sơ/CV (30%).");
        return result;
    }

    private void analyzeAndScore(SkillVerification verification) {
        Set<String> extractedSkills = new LinkedHashSet<>();
        if (StringUtils.hasText(verification.getSkillName())) {
            extractedSkills.add(verification.getSkillName());
        }
        extractedSkills.addAll(skillExtractionService.extractSkills(verificationAnalysisText(verification)));
        if (!extractedSkills.isEmpty()) {
            verification.setSkillName(normalizeSkill(extractedSkills.iterator().next()));
            verification.setExtractedSkills(
                    extractedSkills.stream().map(this::normalizeSkill).distinct().collect(Collectors.joining(", ")));
        }

        List<String> trustSignals = new ArrayList<>();
        List<String> riskSignals = new ArrayList<>();
        int score = 35;

        VerificationSourceType sourceType = resolveSourceType(parseSourceType(verification.getSourceType()));
        score += switch (sourceType) {
            case GITHUB -> 25;
            case HACKERRANK, COURSERA, LINKEDIN_LEARNING -> 22;
            case UDEMY -> 18;
            case CERTIFICATE_UPLOAD, CERTIFICATE_LINK -> 15;
            case OTHER -> 8;
        };
        trustSignals.add("Nguồn xác thực: " + sourceType.name());

        if (StringUtils.hasText(verification.getSourceUrl()) && verification.getSourceUrl().startsWith("https://")) {
            score += 12;
            trustSignals.add("Link HTTPS có thể kiểm tra trực tuyến");
        } else if (StringUtils.hasText(verification.getSourceUrl())) {
            score += 5;
            riskSignals.add("Link không dùng HTTPS");
        }

        if (StringUtils.hasText(verification.getArtifactPath())) {
            score += 12;
            trustSignals.add("Có file chứng chỉ/minh chứng đã upload");
        }
        if (StringUtils.hasText(verification.getEvidenceText()) && verification.getEvidenceText().length() >= 80) {
            score += 10;
            trustSignals.add("Mô tả minh chứng đủ chi tiết để AI/rule-based phân tích");
        }
        if (sourceType == VerificationSourceType.GITHUB && containsAny(verificationAnalysisText(verification),
                "repository", "commit", "pull request", "spring", "docker", "react")) {
            score += 8;
            trustSignals.add("Minh chứng GitHub có dấu hiệu dự án/kho mã nguồn thực tế");
        }
        if (extractedSkills.isEmpty()) {
            score -= 15;
            riskSignals.add("Chưa trích xuất được kỹ năng rõ ràng từ minh chứng");
        }
        if (!StringUtils.hasText(verification.getSourceUrl()) && !StringUtils.hasText(verification.getArtifactPath())) {
            score -= 10;
            riskSignals.add("Thiếu link hoặc file chứng chỉ để đối soát");
        }

        verification.setConfidenceScore(Math.max(0, Math.min(100, score)));
        verification.setTrustSignals(String.join("\n", trustSignals));
        verification
                .setRiskSignals(riskSignals.isEmpty() ? "Không phát hiện rủi ro lớn" : String.join("\n", riskSignals));
        verification.setEvidenceSummary(buildEvidenceSummary(verification, sourceType, extractedSkills));
        verification.setLastAnalyzedAt(LocalDateTime.now());
    }

    private String resolveInitialStatus(SkillVerification verification) {
        return nullToZero(verification.getConfidenceScore()) >= 70
                ? VerificationStatus.PENDING_VERIFICATION.name()
                : VerificationStatus.SUBMITTED.name();
    }

    private VerifiedSkillItem toVerifiedSkillItem(String skill, List<SkillVerification> verifications) {
        List<String> sources = verifications.stream()
                .map(this::sourceLabel)
                .distinct()
                .toList();
        int baseConfidence = (int) Math.round(verifications.stream()
                .mapToInt(v -> nullToZero(v.getConfidenceScore()))
                .average()
                .orElse(0));
        int sourceBonus = Math.min(12, Math.max(0, sources.size() - 1) * 6);
        boolean hasVerified = verifications.stream()
                .anyMatch(v -> VerificationStatus.VERIFIED.name().equals(v.getStatus()));
        int confidence = Math.min(100, baseConfidence + sourceBonus + (hasVerified ? 8 : 0));
        String status = hasVerified ? VerificationStatus.VERIFIED.name() : verifications.get(0).getStatus();
        return new VerifiedSkillItem(skill, sources, confidence, status);
    }

    private Set<String> skillsFromVerification(SkillVerification verification) {
        Set<String> skills = new LinkedHashSet<>();
        if (StringUtils.hasText(verification.getExtractedSkills())) {
            for (String skill : verification.getExtractedSkills().split(",")) {
                if (StringUtils.hasText(skill)) {
                    skills.add(skill.trim());
                }
            }
        }
        if (skills.isEmpty() && StringUtils.hasText(verification.getSkillName())) {
            skills.add(verification.getSkillName());
        }
        return skills;
    }

    private LambdaQueryWrapper<SkillVerification> candidateWrapper(Long candidateId) {
        return new LambdaQueryWrapper<SkillVerification>()
                .eq(SkillVerification::getCandidateId, candidateId)
                .orderByDesc(SkillVerification::getConfidenceScore)
                .orderByDesc(SkillVerification::getCreateTime);
    }

    private StoredArtifact storeCertificateArtifact(Long candidateId, MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "certificate"
                : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String storedName = UUID.randomUUID() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path directory = Paths.get(uploadDir, "skill-verifications", String.valueOf(candidateId)).toAbsolutePath()
                .normalize();
        Path destination = directory.resolve(storedName).normalize();
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            String extractedText = truncate(tika.parseToString(destination.toFile()), 8000);
            return new StoredArtifact(destination.toString(), extractedText);
        } catch (Exception ex) {
            throw new BusinessException(ResultCodeEnum.INTERNAL_ERROR,
                    "Unable to store or parse certificate file: " + ex.getMessage());
        }
    }

    private VerificationSourceType parseSourceType(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            return null;
        }
        try {
            return VerificationSourceType.valueOf(sourceType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return VerificationSourceType.OTHER;
        }
    }

    private VerificationSourceType resolveSourceType(VerificationSourceType sourceType) {
        return sourceType == null ? VerificationSourceType.OTHER : sourceType;
    }

    private String resolveProvider(SkillEvidenceRequest request) {
        if (StringUtils.hasText(request.getSourceName())) {
            return request.getSourceName().trim();
        }
        return resolveSourceType(request.getSourceType()).name();
    }

    private String sourceLabel(SkillVerification verification) {
        if (StringUtils.hasText(verification.getSourceName())) {
            return verification.getSourceName();
        }
        if (StringUtils.hasText(verification.getProvider())) {
            return verification.getProvider();
        }
        return verification.getSourceType();
    }

    private String buildEvidenceSummary(SkillVerification verification, VerificationSourceType sourceType,
            Set<String> skills) {
        String source = sourceLabel(verification);
        String skillText = skills.isEmpty() ? "chưa rõ kỹ năng"
                : skills.stream().map(this::normalizeSkill).collect(Collectors.joining(", "));
        return "Minh chứng từ " + sourceType.name() + " (" + source + ") liên quan tới: " + skillText
                + ". Confidence hiện tại: " + verification.getConfidenceScore() + "%";
    }

    private String verificationAnalysisText(SkillVerification verification) {
        return safe(verification.getSkillName()) + " " + safe(verification.getProvider()) + " "
                + safe(verification.getSourceName())
                + " " + safe(verification.getSourceUrl()) + " " + safe(verification.getEvidenceText());
    }

    private String appendReviewerNote(String current, String reviewerNote) {
        if (!StringUtils.hasText(reviewerNote)) {
            return current;
        }
        String prefix = !StringUtils.hasText(current) ? "" : current + "\n";
        return prefix + "Reviewer note: " + reviewerNote;
    }

    private String joinEvidence(String current, String extracted) {
        if (!StringUtils.hasText(extracted)) {
            return current;
        }
        String prefix = !StringUtils.hasText(current) ? "" : current + "\n";
        return prefix + "Extracted certificate text:\n" + extracted;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private boolean containsAny(String text, String... tokens) {
        String lower = safe(text).toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (lower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private SkillVerification getRequired(Long id) {
        SkillVerification verification = verificationMapper.selectById(id);
        if (verification == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Skill verification not found");
        }
        return verification;
    }

    private String normalizeSkill(String skill) {
        return trimToNull(skill) == null ? "unknown" : skill.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void normalizeLegacyPayload(SkillVerification verification) {
        if (verification == null) {
            return;
        }

        verification.setSkillName(trimToNull(verification.getSkillName()));
        verification.setProvider(trimToNull(verification.getProvider()));
        verification.setSourceName(trimToNull(verification.getSourceName()));
        verification.setSourceUrl(normalizeUrl(verification.getSourceUrl()));
        verification.setCertificateUrl(normalizeUrl(verification.getCertificateUrl()));
        verification.setEvidenceText(trimToNull(verification.getEvidenceText()));

        if (!StringUtils.hasText(verification.getSourceType())) {
            verification.setSourceType(VerificationSourceType.OTHER.name());
        }
    }

    private String normalizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        url = url.trim();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        return url;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record StoredArtifact(String path, String extractedText) {
    }

}