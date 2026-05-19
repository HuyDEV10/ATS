package com.dacn.ATS.module.resume.service;

import com.dacn.ATS.module.resume.dto.ParsedResumeInfo;

import java.nio.file.Path;

public interface ResumeParserService {
    ParsedResumeInfo parse(Path filePath);
}