package com.dacn.ATS.module.ai.service.impl;

import com.dacn.ATS.module.ai.service.CvParserService;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class CvParserServiceImpl implements CvParserService {

    @Override
    public String parseToText(String filePath) {
        try {
            Tika tika = new Tika();
            return tika.parseToString(new File(filePath));
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc nội dung CV: " + e.getMessage());
        }
    }
}