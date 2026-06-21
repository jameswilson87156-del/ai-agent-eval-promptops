package com.promptops.evalconsole.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("generation_trace")
public class GenerationTraceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evalRunId;
    private Long evalCaseId;
    private Long promptVersionId;
    private String inputSummary;
    private String outputSummary;
    private Integer latencyMs;
    private String status;
    private String errorReason;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEvalRunId() {
        return evalRunId;
    }

    public void setEvalRunId(Long evalRunId) {
        this.evalRunId = evalRunId;
    }

    public Long getEvalCaseId() {
        return evalCaseId;
    }

    public void setEvalCaseId(Long evalCaseId) {
        this.evalCaseId = evalCaseId;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public void setPromptVersionId(Long promptVersionId) {
        this.promptVersionId = promptVersionId;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public void setOutputSummary(String outputSummary) {
        this.outputSummary = outputSummary;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
