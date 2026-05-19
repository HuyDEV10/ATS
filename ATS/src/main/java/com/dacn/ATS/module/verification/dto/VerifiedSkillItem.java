package com.dacn.ATS.module.verification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifiedSkillItem {
    private String skillName;
    private List<String> sources = new ArrayList<>();
    private int confidenceScore;
    private String status;
}