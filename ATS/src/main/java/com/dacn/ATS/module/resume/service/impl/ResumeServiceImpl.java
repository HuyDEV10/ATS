package com.dacn.ATS.module.resume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.resume.dto.ParsedResumeInfo;
import com.dacn.ATS.module.resume.dto.ResumeConflictResult;
import com.dacn.ATS.module.resume.entity.Resume;
import com.dacn.ATS.module.resume.mapper.ResumeMapper;
import com.dacn.ATS.module.resume.service.ResumeParserService;
import com.dacn.ATS.module.resume.service.ResumeService;
import com.dacn.ATS.module.resume.util.FileValidationUtil;
import com.dacn.ATS.module.securityscan.dto.FileScanResult;
import com.dacn.ATS.module.securityscan.service.FileSecurityScanner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.StringJoiner;

@Service
public class ResumeServiceImpl implements ResumeService {

    @Value("${resume.upload-dir:./uploads}")
    private String uploadDir;

    private final ResumeMapper resumeMapper;
    private final CandidateMapper candidateMapper;
    private final FileSecurityScanner fileSecurityScanner;
    private final ResumeParserService resumeParserService;

    public ResumeServiceImpl(
            ResumeMapper resumeMapper,
            CandidateMapper candidateMapper,
            FileSecurityScanner fileSecurityScanner,
            ResumeParserService resumeParserService) {
        this.resumeMapper = resumeMapper;
        this.candidateMapper = candidateMapper;
        this.fileSecurityScanner = fileSecurityScanner;
        this.resumeParserService = resumeParserService;
    }

    @Override
    public Resume uploadResumeForCompany(MultipartFile file, Long companyId) throws IOException {
        if (!FileValidationUtil.isValidFile(file)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Only PDF, DOC, DOCX files under 10MB are allowed");
        }

        FileScanResult scanResult = fileSecurityScanner.scan(file);
        if (!scanResult.isSafe()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, scanResult.getMessage());
        }

        String md5 = DigestUtils.md5DigestAsHex(file.getInputStream());

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = file.getOriginalFilename();
        String ext = FileValidationUtil.getFileExtension(originalName);
        String savedName = System.currentTimeMillis() + "_public_apply" + ext;
        Path filePath = uploadPath.resolve(savedName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Resume resume = new Resume();
        resume.setCompanyId(companyId);
        resume.setFileName(originalName);
        resume.setFilePath(filePath.toString());
        resume.setFileHash(md5);
        resume.setUploadedBy(null);
        resume.setUploadTime(LocalDateTime.now());
        resume.setParseStatus("PENDING");
        resume.setDeleted(0);

        resumeMapper.insert(resume);

        return parseResume(resume.getId());
    }

    @Override
    public Resume uploadResume(MultipartFile file, Long userId) throws IOException {
        if (!FileValidationUtil.isValidFile(file)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Only PDF, DOC, DOCX files under 10MB are allowed");
        }

        FileScanResult scanResult = fileSecurityScanner.scan(file);
        if (!scanResult.isSafe()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, scanResult.getMessage());
        }

        String md5 = DigestUtils.md5DigestAsHex(file.getInputStream());

        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getFileHash, md5);

        if (resumeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Duplicate file already uploaded");
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = file.getOriginalFilename();
        String ext = FileValidationUtil.getFileExtension(originalName);
        String savedName = System.currentTimeMillis() + "_" + userId + ext;
        Path filePath = uploadPath.resolve(savedName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Resume resume = new Resume();
        resume.setFileName(originalName);
        resume.setFilePath(filePath.toString());
        resume.setFileHash(md5);
        resume.setUploadedBy(userId);
        resume.setUploadTime(LocalDateTime.now());
        resume.setParseStatus("PENDING");
        resume.setDeleted(0);

        resumeMapper.insert(resume);

        return parseResume(resume.getId());
    }

    @Override
    public Resume parseResume(Long resumeId) {
        Resume resume = getResumeById(resumeId);

        try {
            ParsedResumeInfo parsedInfo = resumeParserService.parse(Paths.get(resume.getFilePath()));

            resume.setParsedText(parsedInfo.getRawText());
            resume.setParseStatus("PARSED");
            resume.setParseError(null);
            resume.setParsedAt(LocalDateTime.now());

            resumeMapper.updateById(resume);
            return resumeMapper.selectById(resumeId);
        } catch (Exception e) {
            resume.setParseStatus("FAILED");
            resume.setParseError(e.getMessage());
            resume.setParsedAt(LocalDateTime.now());

            resumeMapper.updateById(resume);
            return resumeMapper.selectById(resumeId);
        }
    }

    @Override
    public ResumeConflictResult compareResumeWithCandidate(Long candidateId) {
        Candidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Candidate not found");
        }

        if (candidate.getResumeId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Candidate has no resume");
        }

        Resume resume = getResumeById(candidate.getResumeId());

        if (!"PARSED".equals(resume.getParseStatus())) {
            resume = parseResume(resume.getId());
        }

        ParsedResumeInfo parsedInfo = resumeParserService.parse(Paths.get(resume.getFilePath()));

        ResumeConflictResult result = new ResumeConflictResult();
        result.setCandidateId(candidate.getId());
        result.setResumeId(resume.getId());

        result.setFormEmail(candidate.getEmail());
        result.setCvEmail(parsedInfo.getEmail());

        result.setFormPhone(candidate.getPhone());
        result.setCvPhone(parsedInfo.getPhone());

        result.setFormSkills(candidate.getSkills());
        result.setCvSkills(joinSkills(parsedInfo));

        int score = 0;

        if (StringUtils.hasText(candidate.getEmail()) && StringUtils.hasText(parsedInfo.getEmail())
                && !candidate.getEmail().trim().equalsIgnoreCase(parsedInfo.getEmail().trim())) {
            score += 3;
            result.getWarnings().add("Email in CV is different from candidate form");
        }

        if (StringUtils.hasText(candidate.getPhone()) && StringUtils.hasText(parsedInfo.getPhone())
                && !normalizePhone(candidate.getPhone()).equals(normalizePhone(parsedInfo.getPhone()))) {
            score += 2;
            result.getWarnings().add("Phone number in CV is different from candidate form");
        }

        if (StringUtils.hasText(candidate.getSkills()) && parsedInfo.getSkills() != null
                && !parsedInfo.getSkills().isEmpty()) {
            String formSkills = candidate.getSkills().toLowerCase();
            boolean hasAnySkillMatch = parsedInfo.getSkills()
                    .stream()
                    .anyMatch(skill -> formSkills.contains(skill.toLowerCase()));

            if (!hasAnySkillMatch) {
                score += 1;
                result.getWarnings().add("Skills in CV do not match skills in candidate form");
            }
        }

        if (result.getWarnings().isEmpty()) {
            result.setConflictLevel("NONE");
        } else if (score >= 3) {
            result.setConflictLevel("HIGH");
        } else if (score == 2) {
            result.setConflictLevel("MEDIUM");
        } else {
            result.setConflictLevel("LOW");
        }

        return result;
    }

    @Override
    public Resume getResumeById(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Resume not found");
        }
        return resume;
    }

    @Override
    public Page<Resume> pageResumes(int page, int size, String keyword) {
        Page<Resume> pageObj = new Page<>(page, size);

        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Resume::getFileName, keyword);
        }

        wrapper.orderByDesc(Resume::getUploadTime);

        return resumeMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public void deleteResume(Long id) {
        Resume resume = getResumeById(id);

        try {
            Path path = Paths.get(resume.getFilePath());
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException ignored) {
        }

        resumeMapper.deleteById(id);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }

        return phone.replaceAll("[^0-9+]", "");
    }

    private String joinSkills(ParsedResumeInfo parsedInfo) {
        if (parsedInfo.getSkills() == null || parsedInfo.getSkills().isEmpty()) {
            return null;
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (String skill : parsedInfo.getSkills()) {
            joiner.add(skill);
        }

        return joiner.toString();
    }
}