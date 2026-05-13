package com.dacn.ATS.module.application.enums;

import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;

public final class ApplicationStatusTransitionValidator {

    private ApplicationStatusTransitionValidator() {
    }

    public static ApplicationStatus parse(String value) {
        if (!ApplicationStatus.isValid(value)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Invalid application status: " + value);
        }
        return ApplicationStatus.valueOf(value);
    }

    public static void validate(String current, String next) {
        ApplicationStatus currentStatus = parse(current);
        ApplicationStatus nextStatus = parse(next);
        if (!currentStatus.canTransitionTo(nextStatus)) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "Invalid application status transition: " + current + " -> " + next);
        }
    }
}