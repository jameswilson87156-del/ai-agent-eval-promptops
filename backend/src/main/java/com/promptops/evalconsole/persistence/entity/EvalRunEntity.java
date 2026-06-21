package com.promptops.evalconsole.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("eval_run")
public class EvalRunEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String runKey;
    private Long promptVersionId;
    private Long datasetId;
    private String status;
    private Integer totalCases;
    private Integer passedCases;
    private Integer failedCases;
    private Integer riskCaseCount;
    private BigDecimal passRate;
    private Integer averageLatencyMs;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunKey() {
        return runKey;
    }

    public void setRunKey(String runKey) {
        this.runKey = runKey;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public void setPromptVersionId(Long promptVersionId) {
        this.promptVersionId = promptVersionId;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalCases() {
        return totalCases;
    }

    public void setTotalCases(Integer totalCases) {
        this.totalCases = totalCases;
    }

    public Integer getPassedCases() {
        return passedCases;
    }

    public void setPassedCases(Integer passedCases) {
        this.passedCases = passedCases;
    }

    public Integer getFailedCases() {
        return failedCases;
    }

    public void setFailedCases(Integer failedCases) {
        this.failedCases = failedCases;
    }

    public Integer getRiskCaseCount() {
        return riskCaseCount;
    }

    public void setRiskCaseCount(Integer riskCaseCount) {
        this.riskCaseCount = riskCaseCount;
    }

    public BigDecimal getPassRate() {
        return passRate;
    }

    public void setPassRate(BigDecimal passRate) {
        this.passRate = passRate;
    }

    public Integer getAverageLatencyMs() {
        return averageLatencyMs;
    }

    public void setAverageLatencyMs(Integer averageLatencyMs) {
        this.averageLatencyMs = averageLatencyMs;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
