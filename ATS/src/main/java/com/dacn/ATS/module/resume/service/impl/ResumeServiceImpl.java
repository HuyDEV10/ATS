package com.dacn.ATS.module.resume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.resume.entity.Resume;
import com.dacn.ATS.module.resume.mapper.ResumeMapper;
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

@Service
public class ResumeServiceImpl implements ResumeService {

    @Value("${resume.upload-dir:./uploads}")
    private String uploadDir;

    private final ResumeMapper resumeMapper;
    private final FileSecurityScanner fileSecurityScanner;

    public ResumeServiceImpl(ResumeMapper resumeMapper, FileSecurityScanner fileSecurityScanner) {
        this.resumeMapper = resumeMapper;
        this.fileSecurityScanner = fileSecurityScanner;
    }

    @Override
    public Resume uploadResume(MultipartFile file, Long userId) throws IOException {
        FileScanResult scanResult = fileSecurityScanner.scan(file);
        if (!scanResult.isSafe()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, scanResult.getMessage());
        }

        // Tính MD5
        String md5 = DigestUtils.md5DigestAsHex(file.getInputStream());

        // Kiểm tra trùng MD5
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getFileHash, md5);
        if (resumeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Duplicate file already uploaded");
        }

        // Tạo thư mục uploads nếu chưa có
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Lưu file với tên an toàn: timestamp + userId + originalExtension
        String originalName = file.getOriginalFilename();
        String ext = FileValidationUtil.getFileExtension(originalName);
        String savedName = System.currentTimeMillis() + "_" + userId + ext;
        Path filePath = uploadPath.resolve(savedName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Lưu vào DB
        Resume resume = new Resume();
        resume.setFileName(originalName);
        resume.setFilePath(filePath.toString());
        resume.setFileHash(md5);
        resume.setUploadedBy(userId);
        resume.setUploadTime(LocalDateTime.now());
        resume.setParseStatus("PENDING");
        resume.setDeleted(0);
        resumeMapper.insert(resume);
        return resume;
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
        // Xóa file vật lý
        try {
            Path path = Paths.get(resume.getFilePath());
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            // log lỗi nhưng vẫn xóa DB
        }
        resumeMapper.deleteById(id);
    }
}