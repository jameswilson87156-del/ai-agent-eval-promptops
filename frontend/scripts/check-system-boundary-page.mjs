import { checkShowcasePage } from './page-check-common.mjs'

await checkShowcasePage({
  slug: 'system-boundary',
  view: 'boundary',
  frontendPort: 5183,
  shellSelector: '.boundary-shell',
  requiredSelectors: [
    '.boundary-sidebar',
    '.boundary-topbar',
    '.boundary-title',
    '.boundary-summary',
    '.boundary-scope',
    '.boundary-security',
    '.boundary-matrix',
    '.boundary-next',
    '.boundary-flow',
  ],
  countChecks: [
    { selector: '.boundary-summary article', expected: 5, label: 'System Boundary must show five summary cards' },
    { selector: '.boundary-matrix-row', expected: 10, label: 'System Boundary must show ten matrix rows' },
  ],
})
