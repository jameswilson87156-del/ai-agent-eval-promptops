import { checkShowcasePage } from './page-check-common.mjs'

await checkShowcasePage({
  slug: 'batch-evaluation',
  view: 'batch',
  frontendPort: 5177,
  shellSelector: '.batch-page-shell',
  requiredSelectors: [
    '.batch-topbar',
    '.batch-sidebar',
    '.batch-title-row',
    '.batch-config-card',
    '.batch-run-card',
    '.batch-metrics-card',
    '.batch-matrix-card',
    '.batch-timeline-card',
    '.batch-failure-card',
    '.batch-provider-card',
    '.batch-loop-card',
  ],
  countChecks: [
    { selector: '.batch-timeline article', expected: 7, label: 'Batch timeline must show all 7 stages' },
    { selector: '.batch-matrix button[role="row"]', expected: 7, label: 'Batch matrix must show all 7 example cases' },
  ],
  async extraChecks(page) {
    await page.locator('.batch-matrix button.selected', { hasText: 'case_093' }).waitFor({ state: 'visible' })
    await page.locator('.batch-matrix button', { hasText: 'case_094' }).click()
    await page.locator('.batch-failure-card', { hasText: 'case_094' }).waitFor({ state: 'visible' })
    await page.locator('.batch-matrix button', { hasText: 'case_093' }).click()
    await page.locator('.batch-failure-card', { hasText: '缺少 confidence 字段' }).waitFor({ state: 'visible' })
  },
})
