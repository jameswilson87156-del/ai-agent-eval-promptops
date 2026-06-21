export type PromptStatus = 'Draft' | 'Candidate' | 'Approved'

export type CaseResultStatus = 'pass' | 'fail' | 'format-error' | 'risk-missing'

export interface Summary {
  promptVersions: number
  evalRuns: number
  testCases: number
  latestPassRate: number
  averageLatencyMs: number
  pendingReviews: number
  highRiskCases: number
}

export interface PromptVariable {
  name: string
  description: string
  required: boolean
}

export interface PromptProfile {
  id: number
  key: string
  name: string
  tags: string[]
  versionLabels: string[]
  currentVersionId: number
  currentVersion: string
  status: PromptStatus
  owner: string
  updatedAt: string
  template: string
  variables: PromptVariable[]
  scoringRules: string[]
  outputRequirements: string[]
  datasetId: number
}

export interface RuleCheck {
  ruleType: string
  expected: string
  passed: boolean
  explanation: string
}

export interface EvalCase {
  resultId: number
  caseId: number
  caseKey: string
  name: string
  dataset: string
  input: string
  expected: string
  result: CaseResultStatus
  riskLevel: 'normal' | 'medium' | 'high' | 'critical'
  reviewRequired: boolean
  score: number
  latencyMs: number
  reason: string
  outputSummary: string
  mockOutput: string
  trace: string[]
  ruleChecks: RuleCheck[]
}

export interface EvalRun {
  id: number
  runKey: string
  name: string
  promptId: number
  promptVersionId: number
  datasetId: number
  promptSnapshot: string
  status: 'queued' | 'running' | 'finished'
  totalCases: number
  passedCases: number
  failedCases: number
  riskCaseCount: number
  passRate: number
  averageLatencyMs: number
  cases: EvalCase[]
}

export interface VersionComparison {
  promptId: number
  fromVersion: string
  toVersion: string
  passRateBefore: number
  passRateAfter: number
  latencyBeforeMs: number
  latencyAfterMs: number
  regressionRisk: 'low' | 'medium' | 'high'
  topFailureReasons: string[]
}

export interface GenerationTrace {
  id: number
  evalRunId: number
  evalCaseId: number
  promptVersionId: number
  inputSummary: string
  outputSummary: string
  latencyMs: number
  status: string
  errorReason: string | null
  createdAt: string
}
