package com.dacn.ATS.module.interview.enums;

import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;

public enum InterviewStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    public static InterviewStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Interview status is required");
        }

        try {
            return InterviewStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Invalid interview status: " + value);
        }
    }
}