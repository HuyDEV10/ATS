package com.dacn.ATS.module.ai.client;

public class AiCompletionResponse {
    private String content;
    private String model;
    private boolean successful;
    private String errorMessage;

    public static AiCompletionResponse skipped(String errorMessage) {
        AiCompletionResponse response = new AiCompletionResponse();
        response.setSuccessful(false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}