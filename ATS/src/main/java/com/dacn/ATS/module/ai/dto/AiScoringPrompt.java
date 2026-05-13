package com.dacn.ATS.module.ai.dto;

public class AiScoringPrompt {
    private final String version;
    private final String systemPrompt;
    private final String userPrompt;
    private final String responseSchema;

    public AiScoringPrompt(String version, String systemPrompt, String userPrompt, String responseSchema) {
        this.version = version;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.responseSchema = responseSchema;
    }

    public String getVersion() {
        return version;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public String getResponseSchema() {
        return responseSchema;
    }
}