package com.promptops.evalconsole.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/** A selectable agent execution strategy. Implementations must never expose credentials in reports. */
public interface AgentProvider {
    String providerType();

    String modelName();

    String defaultAgentConfigVersion();

    String defaultPromptVersion();

    boolean isConfigured();

    /** Non-secret settings that participate in the run configuration fingerprint. */
    Map<String, Object> fingerprintSettings();

    Map<String, Object> publicStatus();

    Map<String, Object> execute(JsonNode input, List<Map<String, Object>> trace) throws Exception;
}
