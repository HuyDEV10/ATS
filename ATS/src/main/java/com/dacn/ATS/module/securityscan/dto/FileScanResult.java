package com.dacn.ATS.module.securityscan.dto;

public class FileScanResult {
    private final boolean safe;
    private final String detectedContentType;
    private final String message;

    private FileScanResult(boolean safe, String detectedContentType, String message) {
        this.safe = safe;
        this.detectedContentType = detectedContentType;
        this.message = message;
    }

    public static FileScanResult safe(String detectedContentType) {
        return new FileScanResult(true, detectedContentType, "File passed local security checks");
    }

    public static FileScanResult unsafe(String detectedContentType, String message) {
        return new FileScanResult(false, detectedContentType, message);
    }

    public boolean isSafe() {
        return safe;
    }

    public String getDetectedContentType() {
        return detectedContentType;
    }

    public String getMessage() {
        return message;
    }
}