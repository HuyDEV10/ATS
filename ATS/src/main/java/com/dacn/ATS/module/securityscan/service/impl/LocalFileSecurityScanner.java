package com.dacn.ATS.module.securityscan.service.impl;

import com.dacn.ATS.module.resume.util.FileValidationUtil;
import com.dacn.ATS.module.securityscan.dto.FileScanResult;
import com.dacn.ATS.module.securityscan.exception.FileSecurityScanException;
import com.dacn.ATS.module.securityscan.service.FileSecurityScanner;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
public class LocalFileSecurityScanner implements FileSecurityScanner {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final Tika tika = new Tika();

    @Override
    public FileScanResult scan(MultipartFile file) {
        if (!FileValidationUtil.hasAllowedSize(file)) {
            return FileScanResult.unsafe(null, "File is empty or larger than upload policy");
        }
        if (!FileValidationUtil.hasAllowedExtension(file.getOriginalFilename())) {
            return FileScanResult.unsafe(null, "File extension is not allowed");
        }

        String detectedContentType = detect(file);
        if (!ALLOWED_CONTENT_TYPES.contains(detectedContentType)) {
            return FileScanResult.unsafe(detectedContentType, "Detected file type is not allowed");
        }
        return FileScanResult.safe(detectedContentType);
    }

    private String detect(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        } catch (IOException e) {
            throw new FileSecurityScanException("Cannot scan uploaded file: " + e.getMessage());
        }
    }
}