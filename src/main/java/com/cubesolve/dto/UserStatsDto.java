package com.cubesolve.dto;

import java.util.List;

/**
 * DTO for user dashboard statistics computed directly from MySQL.
 */
public class UserStatsDto {

    private long totalSolves;
    private Long bestTimeMs;
    private Double averageTimeMs;
    private Integer bestMoveCount;
    private List<SolveRecordDto> recentSolves;

    public UserStatsDto() {}

    public UserStatsDto(long totalSolves, Long bestTimeMs, Double averageTimeMs, Integer bestMoveCount, List<SolveRecordDto> recentSolves) {
        this.totalSolves = totalSolves;
        this.bestTimeMs = bestTimeMs;
        this.averageTimeMs = averageTimeMs;
        this.bestMoveCount = bestMoveCount;
        this.recentSolves = recentSolves;
    }

    public long getTotalSolves() {
        return totalSolves;
    }

    public void setTotalSolves(long totalSolves) {
        this.totalSolves = totalSolves;
    }

    public Long getBestTimeMs() {
        return bestTimeMs;
    }

    public void setBestTimeMs(Long bestTimeMs) {
        this.bestTimeMs = bestTimeMs;
    }

    public Double getAverageTimeMs() {
        return averageTimeMs;
    }

    public void setAverageTimeMs(Double averageTimeMs) {
        this.averageTimeMs = averageTimeMs;
    }

    public Integer getBestMoveCount() {
        return bestMoveCount;
    }

    public void setBestMoveCount(Integer bestMoveCount) {
        this.bestMoveCount = bestMoveCount;
    }

    public List<SolveRecordDto> getRecentSolves() {
        return recentSolves;
    }

    public void setRecentSolves(List<SolveRecordDto> recentSolves) {
        this.recentSolves = recentSolves;
    }
}
