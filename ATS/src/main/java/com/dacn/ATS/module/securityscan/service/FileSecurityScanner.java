package com.dacn.ATS.module.securityscan.service;

import com.dacn.ATS.module.securityscan.dto.FileScanResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileSecurityScanner {
    FileScanResult scan(MultipartFile file);
}