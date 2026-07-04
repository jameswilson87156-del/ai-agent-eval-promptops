import { checkShowcasePage } from './page-check-common.mjs'

await checkShowcasePage({
  slug: 'trace-detail',
  view: 'trace',
  frontendPort: 5178,
  shellSelector: '.trace-shell',
  requiredSelectors: [
    '.trace-topbar',
    '.trace-sidebar',
    '.trace-title-row',
    '.trace-context-strip',
    '.trace-summary-card',
    '.trace-prompt-card',
    '.trace-schema-card',
    '.trace-score-card',
    '.trace-fix-card',
    '.trace-timeline-panel',
    '.trace-review-card',
    '.trace-provider-card',
    '.trace-flow-card',
  ],
  countChecks: [
    { selector: '.trace-timeline article', expected: 7, label: 'Trace timeline must show all 7 stages' },
    { selector: '.trace-score-tags span', expected: 6, label: 'Score trace node must show six score labels' },
  ],
  async extraChecks(page) {
    await page.locator('.trace-timeline article[data-selected="true"]', { hasText: 'Failure Case' }).waitFor({ state: 'visible' })
    await page.locator('.trace-timeline article[data-state="warning"]', { hasText: 'Human Review' }).waitFor({ state: 'visible' })
    await page.locator('.trace-context-strip', { hasText: 'run_1022' }).waitFor({ state: 'visible' })
    await page.locator('.trace-provider-card', { hasText: '非生产环境' }).waitFor({ state: 'visible' })
  },
})
