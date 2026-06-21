package com.promptops.evalconsole.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("eval_case_result")
public class EvalCaseResultEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evalRunId;
    private Long evalCaseId;
    private String resultStatus;
    private BigDecimal score;
    private Integer latencyMs;
    private String failureReasonsJson;
    private String outputSummary;
    private String mockOutput;
    private String explanation;
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

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getFailureReasonsJson() {
        return failureReasonsJson;
    }

    public void setFailureReasonsJson(String failureReasonsJson) {
        this.failureReasonsJson = failureReasonsJson;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public void setOutputSummary(String outputSummary) {
        this.outputSummary = outputSummary;
    }

    public String getMockOutput() {
        return mockOutput;
    }

    public void setMockOutput(String mockOutput) {
        this.mockOutput = mockOutput;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
