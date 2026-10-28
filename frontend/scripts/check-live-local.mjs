import { chromium } from 'playwright';
import { mkdir, access, readFile } from 'node:fs/promises';
import path from 'node:path';

const baseUrl = process.env.AGENT_EVAL_BASE_URL || 'http://localhost:5179';
const outputDir = process.env.AGENT_EVAL_OUTPUT_DIR
  ? path.resolve(process.env.AGENT_EVAL_OUTPUT_DIR)
  : path.resolve(process.cwd(), '../../../outputs');
await mkdir(outputDir, { recursive: true });

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 920 }, acceptDownloads: true });
const pageErrors = [];
page.on('pageerror', error => pageErrors.push(error.message));

function assert(condition, message) {
  if (!condition) throw new Error(message);
}
function sameRunData(run) {
  return run.cases.map(item => [item.case_id, item.case_status, item.actual_output]);
}

try {
  const statusResponse = await fetch(baseUrl + '/api/agent-eval/provider/status');
  assert(statusResponse.ok, 'Provider status endpoint returned ' + statusResponse.status);
  const providerStatus = await statusResponse.json();
  const serializedStatus = JSON.stringify(providerStatus).toLowerCase();
  assert(!serializedStatus.includes('api_key') && !serializedStatus.includes('authorization'), 'Provider status exposed a credential-shaped field');
  const mockProvider = providerStatus.providers.find(item => item.provider_type === 'mock');
  const modelProvider = providerStatus.providers.find(item => item.provider_type === 'openai_compatible');
  assert(mockProvider?.configured === true, 'Deterministic Mock provider is not ready');
  let providerPreflight = 'not-run';
  if (modelProvider && modelProvider.configured === false) {
    const rejected = await fetch(baseUrl + '/api/agent-eval/runs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ provider_type: 'openai_compatible', prompt_version: 'browser-preflight-check' }),
    });
    assert(rejected.status === 409, 'Unconfigured real Provider should be rejected before a run; got ' + rejected.status);
    providerPreflight = '409 rejected without provider request';
  } else {
    providerPreflight = 'skipped because provider is configured; no real model call was issued';
  }

  await page.goto(baseUrl + '/');
  await page.getByRole('heading', { name: 'Agent 评测总览' }).waitFor();
  const previousRunCount = (await (await fetch(baseUrl + '/api/agent-eval/runs')).json()).length;

  const firstPostPromise = page.waitForResponse(response => response.url() === baseUrl + '/api/agent-eval/runs' && response.request().method() === 'POST');
  await page.getByRole('button', { name: /运行完整测试集/ }).click();
  const firstPost = await firstPostPromise;
  assert(firstPost.ok(), 'Mock execution POST returned ' + firstPost.status());
  const firstReport = await firstPost.json();
  assert(firstReport.run_mode === 'mock' && firstReport.provider_type === 'mock', 'Run was not explicitly marked as Mock');
  assert(firstReport.cases.length === 26, 'Expected 26 persisted test results');
  assert(firstReport.summary.failed > 0, 'Synthetic failure cases must remain visible');
  const firstSaved = await (await fetch(baseUrl + '/api/agent-eval/runs/' + firstReport.run_id)).json();
  const firstExport = await (await fetch(baseUrl + '/api/agent-eval/runs/' + firstReport.run_id + '/report')).json();
  assert(JSON.stringify(firstReport) === JSON.stringify(firstSaved), 'Saved run endpoint differs from execution report');
  assert(JSON.stringify(firstReport) === JSON.stringify(firstExport), 'JSON report endpoint differs from execution report');
  assert(firstReport.cases.every(item => item.run_id === firstReport.run_id), 'Per-case run ID mismatch');
  const failure = firstReport.cases.find(item => item.case_id === 'agent-014');
  assert(failure?.execution_status === 'success' && failure.case_status === 'failed', 'agent-014 should reproduce as an evaluator failure, not an executor error');
  assert(failure.failure_reason?.length > 0, 'agent-014 failure attribution is missing');
  assert(failure.execution_steps?.length > 0, 'agent-014 Trace is missing');
  assert(failure.actual_output.tool_calls[0].name === 'web_search', 'agent-014 reproduction output changed');

  await page.screenshot({ path: path.join(outputDir, 'agent-eval-run-' + firstReport.run_id + '.png'), fullPage: true });
  await page.getByRole('button', { name: '运行工作台' }).click();
  await page.getByRole('button', { name: /agent-014/ }).click();
  await page.getByText('failed', { exact: true }).first().waitFor();
  await page.getByText('Trace 时间线').waitFor();
  await page.screenshot({ path: path.join(outputDir, 'agent-eval-failure-trace-' + firstReport.run_id + '.png'), fullPage: true });

  await page.getByRole('button', { name: '测试集', exact: true }).click();
  await page.getByRole('textbox', { name: '搜索测试用例' }).fill('agent-014');
  assert(await page.locator('.dataset-table tbody tr').count() === 1, 'Dataset search did not filter to agent-014');
  await page.getByText('agent-014', { exact: true }).click();
  await page.locator('.definition-detail').waitFor();
  await page.screenshot({ path: path.join(outputDir, 'agent-eval-dataset-' + firstReport.run_id + '.png'), fullPage: true });

  await page.getByRole('button', { name: '报告中心', exact: true }).click();
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: /下载 JSON/ }).first().click();
  const download = await downloadPromise;
  const reportPath = path.join(outputDir, 'agent-eval-report-' + firstReport.run_id + '.json');
  try {
    await access(reportPath);
    throw new Error('Refusing to overwrite an existing report: ' + reportPath);
  } catch (error) {
    if (error instanceof Error && error.message.startsWith('Refusing to overwrite')) throw error;
  }
  await download.saveAs(reportPath);
  const downloaded = JSON.parse(await readFile(reportPath, 'utf8'));
  assert(JSON.stringify(downloaded) === JSON.stringify(firstReport), 'Downloaded JSON differs from persisted report');
  await page.screenshot({ path: path.join(outputDir, 'agent-eval-reports-' + firstReport.run_id + '.png'), fullPage: true });

  await page.getByRole('button', { name: '运行历史', exact: true }).click();
  await page.getByText(firstReport.run_id.slice(0, 18), { exact: false }).first().waitFor();
  await page.screenshot({ path: path.join(outputDir, 'agent-eval-history-' + firstReport.run_id + '.png'), fullPage: true });

  await page.getByRole('button', { name: '运行工作台', exact: true }).click();
  await page.getByRole('textbox', { name: 'Prompt 版本' }).fill('mock-prompt-v2-browser');
  await page.getByRole('textbox', { name: 'Agent 配置版本' }).fill('mock-agent-v2-browser');
  const secondPostPromise = page.waitForResponse(response => response.url() === baseUrl + '/api/agent-eval/runs' && response.request().method() === 'POST');
  await page.getByRole('button', { name: /运行完整测试集/ }).click();
  const secondPost = await secondPostPromise;
  assert(secondPost.ok(), 'Second Mock run returned ' + secondPost.status());
  const secondReport = await secondPost.json();
  assert(firstReport.run_id !== secondReport.run_id, 'Run IDs must be unique');
  assert(firstReport.dataset_version === secondReport.dataset_version, 'Comparison runs must use the same dataset version');
  assert(JSON.stringify(sameRunData(firstReport)) === JSON.stringify(sameRunData(secondReport)), 'Repeated Mock run behavior was not deterministic');

  await page.getByRole('button', { name: '回归比较', exact: true }).click();
  await page.getByRole('heading', { name: '回归比较' }).waitFor();
  const baselineSelector = page.getByRole('combobox', { name: '基线运行' });
  await baselineSelector.waitFor();
  await baselineSelector.selectOption(firstReport.run_id);
  await page.getByRole('combobox', { name: '候选运行' }).selectOption(secondReport.run_id);
  const comparisonPromise = page.waitForResponse(response => response.url().includes('/api/agent-eval/comparisons?'));
  await page.getByRole('button', { name: '比较已保存运行' }).click();
  const comparisonResponse = await comparisonPromise;
  assert(comparisonResponse.ok(), 'Comparison endpoint returned ' + comparisonResponse.status());
  const comparison = await comparisonResponse.json();
  assert(comparison.baseline.run_id === firstReport.run_id && comparison.candidate.run_id === secondReport.run_id, 'Comparison did not use the selected persisted runs');
  assert(comparison.comparison_semantics === 'mock_metadata_only_comparison', 'Mock configuration label should not be described as a real-model version regression');
  assert(comparison.is_version_regression === false, 'Mock metadata comparison must not claim a model version regression');
  assert(comparison.case_changes.length === 26 && comparison.category_changes.length === 6, 'Comparison omitted case or category changes');
  await page.locator('.comparison-result').waitFor();
  await page.screenshot({ path: path.join(outputDir, 'agent-eval-comparison-' + secondReport.run_id + '.png'), fullPage: true });

  await page.setViewportSize({ width: 1366, height: 768 });
  const desktopOverflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth);
  assert(!desktopOverflow, '1366px desktop viewport has horizontal overflow');
  await page.getByRole('button', { name: '总览', exact: true }).click();
  await page.screenshot({ path: path.join(outputDir, 'agent-eval-overview-' + secondReport.run_id + '.png'), fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  const mobileOverflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth);
  assert(!mobileOverflow, '390px mobile viewport has horizontal overflow');
  await page.screenshot({ path: path.join(outputDir, 'agent-eval-mobile-' + secondReport.run_id + '.png'), fullPage: true });

  await page.reload();
  await page.getByRole('heading', { name: 'Agent 评测总览' }).waitFor();
  await page.getByRole('button', { name: '运行历史', exact: true }).click();
  await page.getByText(secondReport.run_id.slice(0, 18), { exact: false }).first().waitFor();
  const persistedRuns = await (await fetch(baseUrl + '/api/agent-eval/runs')).json();
  assert(persistedRuns.some(run => run.run_id === firstReport.run_id && run.cases.length === 26), 'Run history did not survive browser refresh');
  assert(persistedRuns.some(run => run.run_id === secondReport.run_id), 'Second saved run missing from backend history');

  await page.getByRole('button', { name: '总览', exact: true }).click();
  await page.route('**/api/agent-eval/runs', async route => {
    if (route.request().method() === 'POST') {
      await route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'simulated service unavailable' }) });
    } else {
      await route.continue();
    }
  });
  const rowsBeforeRejectedRun = await page.locator('tbody tr').count();
  await page.getByRole('button', { name: /运行完整测试集/ }).click();
  await page.getByRole('alert').getByText('simulated service unavailable').waitFor();
  assert(await page.locator('tbody tr').count() === rowsBeforeRejectedRun, 'Rejected run was falsely shown as completed');
  const finalHistory = await (await fetch(baseUrl + '/api/agent-eval/runs')).json();
  assert(finalHistory.length === previousRunCount + 2, 'Rejected frontend request changed persisted run history');
  assert(pageErrors.length === 0, 'Browser JavaScript errors: ' + pageErrors.join('; '));

  console.log(JSON.stringify({
    baseUrl,
    providerStatusSafe: true,
    providerPreflight,
    firstRunId: firstReport.run_id,
    secondRunId: secondReport.run_id,
    priorRunCount: previousRunCount,
    caseCount: firstReport.cases.length,
    mockFailuresPreserved: firstReport.summary.failed,
    reproducibleFailure: failure.case_id,
    jsonMatchesSavedRun: true,
    deterministicRepeat: true,
    comparisonSemantics: comparison.comparison_semantics,
    reloadPersistence: true,
    desktopWidths: [1440, 1366],
    mobileWidth: 390,
    desktopOverflow,
    mobileOverflow,
    failedRequestShown: true,
    pageErrors: pageErrors.length,
    evidenceDirectory: outputDir,
  }));
} finally {
  await browser.close();
}
