import { checkShowcasePage } from './page-check-common.mjs'

await checkShowcasePage({
  slug: 'dataset',
  view: 'dataset',
  frontendPort: 5182,
  shellSelector: '.dataset-shell',
  requiredSelectors: [
    '.dataset-sidebar',
    '.dataset-topbar',
    '.dataset-title',
    '.dataset-metrics',
    '.dataset-list',
    '.dataset-current',
    '.dataset-preview',
    '.dataset-table',
    '.dataset-coverage',
    '.dataset-flow',
  ],
  countChecks: [
    { selector: '.dataset-list button', expected: 4, label: 'Dataset View must show four datasets' },
    { selector: '.dataset-table-row', expected: 7, label: 'Dataset View must show seven samples' },
  ],
})
