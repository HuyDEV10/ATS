package com.dacn.ATS.module.ai.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ats.ai", name = "provider", havingValue = "noop", matchIfMissing = true)
public class NoopAiClient implements AiClient {
    @Override
    public AiCompletionResponse complete(AiCompletionRequest request) {
        return AiCompletionResponse.skipped("No external AI provider is configured; rule-based scoring is used");
    }
}