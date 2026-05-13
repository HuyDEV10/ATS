package com.dacn.ATS.module.ai.service.impl;

import com.dacn.ATS.module.ai.service.SkillExtractionService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RuleBasedSkillExtractionService implements SkillExtractionService {
    private static final List<String> IMPORTANT_SKILLS = List.of(
            "java", "spring", "spring boot", "mysql", "sql",
            "html", "css", "javascript", "react", "vue",
            "python", "machine learning", "ai", "docker",
            "git", "api", "rest", "security", "thymeleaf",
            "mybatis", "database", "testing", "microservices",
            "aws", "linux", "ci/cd", "kubernetes");

    @Override
    public Set<String> extractSkills(String text) {
        Set<String> skills = new HashSet<>();
        String lower = text == null ? "" : text.toLowerCase();
        for (String skill : IMPORTANT_SKILLS) {
            if (lower.contains(skill)) {
                skills.add(skill);
            }
        }
        return skills;
    }
}