import { checkShowcasePage } from './page-check-common.mjs'

await checkShowcasePage({
  slug: 'failure-cases',
  view: 'failure',
  frontendPort: 5181,
  shellSelector: '.failure-shell',
  requiredSelectors: [
    '.failure-sidebar',
    '.failure-topbar',
    '.failure-title',
    '.failure-metrics',
    '.failure-table',
    '.failure-distribution',
    '.failure-insight',
    '.failure-tasks',
    '.failure-flow',
  ],
  countChecks: [
    { selector: '.failure-table-row', expected: 7, label: 'Failure Cases must show seven visible samples' },
    { selector: '.failure-task-row', expected: 4, label: 'Failure Cases must show four repair tasks' },
  ],
})
