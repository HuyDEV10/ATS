package com.dacn.ATS.module.ai.client;

public interface AiClient {
    AiCompletionResponse complete(AiCompletionRequest request);
}