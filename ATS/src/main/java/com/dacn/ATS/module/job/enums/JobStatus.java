package com.dacn.ATS.module.job.enums;

import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;

public enum JobStatus {
    DRAFT,
    PUBLISHED,
    PAUSED,
    CLOSED;

    public static JobStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Job status is required");
        }

        try {
            return JobStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Invalid job status: " + value);
        }
    }

    public boolean canTransitionTo(JobStatus next) {
        if (next == null)
            return false;
        if (this == next)
            return true;

        return switch (this) {
            case DRAFT -> next == PUBLISHED || next == CLOSED;
            case PUBLISHED -> next == PAUSED || next == CLOSED;
            case PAUSED -> next == PUBLISHED || next == CLOSED;
            case CLOSED -> false;
        };
    }
}