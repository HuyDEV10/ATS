package com.dacn.ATS.module.ai.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "ats.ai", name = "provider", havingValue = "spring-ai")
public class SpringAiClient implements AiClient {

    private final ChatClient chatClient;

    @Value("${spring.ai.openai.chat.options.model:unknown}")
    private String model;

    public SpringAiClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public AiCompletionResponse complete(AiCompletionRequest request) {
        try {
            String content = chatClient
                    .prompt()
                    .system(nullToEmpty(request.getSystemPrompt()))
                    .user(buildUserPrompt(request))
                    .call()
                    .content();

            AiCompletionResponse response = new AiCompletionResponse();
            response.setSuccessful(true);
            response.setContent(content);
            response.setModel(model);
            return response;
        } catch (Exception ex) {
            AiCompletionResponse response = new AiCompletionResponse();
            response.setSuccessful(false);
            response.setErrorMessage("Spring AI call failed: " + ex.getMessage());
            response.setModel(model);
            return response;
        }
    }

    private String buildUserPrompt(AiCompletionRequest request) {
        StringBuilder prompt = new StringBuilder();

        if (StringUtils.hasText(request.getUserPrompt())) {
            prompt.append(request.getUserPrompt());
        }

        if (StringUtils.hasText(request.getResponseSchema())) {
            prompt.append("\n\nRESPONSE_FORMAT:\n")
                    .append(request.getResponseSchema());
        }

        if (StringUtils.hasText(request.getPromptVersion())) {
            prompt.append("\n\nPROMPT_VERSION:\n")
                    .append(request.getPromptVersion());
        }

        return prompt.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}