package com.dacn.ATS.module.resume.service.impl;

import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.resume.dto.ParsedResumeInfo;
import com.dacn.ATS.module.resume.service.ResumeParserService;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TikaResumeParserServiceImpl implements ResumeParserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?\\d[\\d\\s().-]{8,}\\d)");

    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d{1,2})\\s*(\\+)?\\s*(years|year|năm)",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> COMMON_SKILLS = List.of(
            "Java", "Spring Boot", "Spring", "MySQL", "SQL", "HTML", "CSS", "JavaScript",
            "React", "Angular", "Vue", "Python", "C++", "C#", "Docker", "Git",
            "REST API", "Microservices", "Hibernate", "MyBatis", "Thymeleaf",
            "Machine Learning", "AI", "Data Analysis");

    @Override
    public ParsedResumeInfo parse(Path filePath) {
        try {
            Tika tika = new Tika();
            String text = tika.parseToString(filePath);

            ParsedResumeInfo info = new ParsedResumeInfo();
            info.setRawText(text);
            info.setEmail(extractEmail(text));
            info.setPhone(extractPhone(text));
            info.setSkills(extractSkills(text));
            info.setExperienceYears(extractExperienceYears(text));

            return info;
        } catch (Exception e) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Cannot parse resume: " + e.getMessage());
        }
    }

    private String extractEmail(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractPhone(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        Matcher matcher = PHONE_PATTERN.matcher(text);
        return matcher.find() ? normalizePhone(matcher.group()) : null;
    }

    private List<String> extractSkills(String text) {
        List<String> found = new ArrayList<>();

        if (!StringUtils.hasText(text)) {
            return found;
        }

        String lowerText = text.toLowerCase();

        for (String skill : COMMON_SKILLS) {
            if (lowerText.contains(skill.toLowerCase())) {
                found.add(skill);
            }
        }

        return found;
    }

    private Integer extractExperienceYears(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        Matcher matcher = EXPERIENCE_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        return phone.replaceAll("[^0-9+]", "");
    }
}