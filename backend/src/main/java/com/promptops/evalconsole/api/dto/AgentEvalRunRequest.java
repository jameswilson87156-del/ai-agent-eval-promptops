package com.promptops.evalconsole.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentEvalRunRequest(
        @JsonProperty("provider_type") String providerType,
        @JsonProperty("prompt_version") String promptVersion,
        @JsonProperty("agent_config_version") String agentConfigVersion) {
}
