package com.promptops.evalconsole.service;

import com.promptops.evalconsole.api.dto.ConsoleDtos.RuleCheckDto;

import java.util.List;

record RuleEvaluation(int score, String status, List<String> failures, List<RuleCheckDto> checks) {
}
