package com.dacn.ATS.module.dashboard.dto;

public class AiScoreDistributionDTO {
    private long strongMatches;
    private long potentialMatches;
    private long weakMatches;
    private long notMatches;

    public long getStrongMatches() {
        return strongMatches;
    }

    public void setStrongMatches(long strongMatches) {
        this.strongMatches = strongMatches;
    }

    public long getPotentialMatches() {
        return potentialMatches;
    }

    public void setPotentialMatches(long potentialMatches) {
        this.potentialMatches = potentialMatches;
    }

    public long getWeakMatches() {
        return weakMatches;
    }

    public void setWeakMatches(long weakMatches) {
        this.weakMatches = weakMatches;
    }

    public long getNotMatches() {
        return notMatches;
    }

    public void setNotMatches(long notMatches) {
        this.notMatches = notMatches;
    }
}