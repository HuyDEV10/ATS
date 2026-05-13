package com.dacn.ATS.module.application.enums;

import java.util.Set;

public enum ApplicationStatus {
    PENDING,
    AI_SCREENED,
    SHORTLISTED,
    INTERVIEW_SCHEDULED,
    INTERVIEWED,
    OFFERED,
    REJECTED;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (ApplicationStatus status : values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean canTransitionTo(ApplicationStatus next) {
        if (next == null) {
            return false;
        }
        if (this == next) {
            return true;
        }
        if (this == REJECTED || this == OFFERED) {
            return false;
        }
        return switch (this) {
            case PENDING -> Set.of(AI_SCREENED, SHORTLISTED, REJECTED).contains(next);
            case AI_SCREENED -> Set.of(SHORTLISTED, INTERVIEW_SCHEDULED, REJECTED).contains(next);
            case SHORTLISTED -> Set.of(INTERVIEW_SCHEDULED, REJECTED).contains(next);
            case INTERVIEW_SCHEDULED -> Set.of(INTERVIEWED, REJECTED).contains(next);
            case INTERVIEWED -> Set.of(OFFERED, REJECTED).contains(next);
            case OFFERED, REJECTED -> false;
        };
    }
}