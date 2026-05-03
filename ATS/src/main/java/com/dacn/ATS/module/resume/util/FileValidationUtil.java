package com.dacn.ATS.module.resume.util;

import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

public class FileValidationUtil {
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public static boolean isValidFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            return false;
        if (file.getSize() > MAX_FILE_SIZE)
            return false;
        String contentType = file.getContentType();
        return ALLOWED_CONTENT_TYPES.contains(contentType);
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains("."))
            return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }
}