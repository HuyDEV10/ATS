package com.dacn.ATS.module.ai.service;

import java.util.Set;

public interface SkillExtractionService {
    Set<String> extractSkills(String text);
}