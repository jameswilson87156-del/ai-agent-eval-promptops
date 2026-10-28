package com.promptops.evalconsole.api;

import com.promptops.evalconsole.service.AgentEvalService;
import com.promptops.evalconsole.api.dto.AgentEvalRunRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent-eval")
@CrossOrigin(origins = {"http://localhost:5174", "http://127.0.0.1:5174", "http://localhost:5179", "http://127.0.0.1:5179", "http://localhost:5181", "http://127.0.0.1:5181"})
public class AgentEvalController {
    private final AgentEvalService service;
    public AgentEvalController(AgentEvalService service) { this.service = service; }

    @GetMapping("/dataset") public Map<String,Object> dataset() { return service.getDataset(); }
    @GetMapping("/provider/status") public Map<String,Object> providerStatus() { return service.getProviderStatus(); }
    @PostMapping("/runs") public Map<String,Object> run(@RequestBody(required = false) AgentEvalRunRequest request) { return service.execute(request); }
    @GetMapping("/runs") public List<Map<String,Object>> runs() { return service.listRuns(); }
    @GetMapping("/runs/{id}") public Map<String,Object> run(@PathVariable String id) { return service.getRun(id); }
    @GetMapping("/comparisons") public Map<String,Object> compare(
            @RequestParam("baseline_run_id") String baselineRunId,
            @RequestParam("candidate_run_id") String candidateRunId) {
        return service.compareRuns(baselineRunId, candidateRunId);
    }
    @GetMapping(value="/runs/{id}/report", produces=MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> report(@PathVariable String id) { return service.getRun(id); }
}
