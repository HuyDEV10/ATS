package com.dacn.ATS.module.resume.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.resume.entity.Resume;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface ResumeService {
    Resume uploadResume(MultipartFile file, Long userId) throws IOException;

    Resume getResumeById(Long id);

    Page<Resume> pageResumes(int page, int size, String keyword);

    void deleteResume(Long id);
}
