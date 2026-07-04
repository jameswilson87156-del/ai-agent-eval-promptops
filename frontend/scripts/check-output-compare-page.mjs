import { checkShowcasePage } from './page-check-common.mjs'

await checkShowcasePage({
  slug: 'output-compare',
  view: 'compare',
  frontendPort: 5179,
  shellSelector: '.compare-shell',
  requiredSelectors: [
    '.compare-sidebar',
    '.compare-topbar',
    '.compare-title-row',
    '.compare-context',
    '.compare-metrics',
    '.compare-diff',
    '.compare-schema',
    '.compare-risk',
    '.compare-decision',
    '.compare-recent',
    '.compare-flow',
  ],
  countChecks: [
    { selector: '.compare-output', expected: 2, label: 'Output Compare must show both outputs' },
    { selector: '.compare-flow button', expected: 6, label: 'Output Compare flow must show six stages' },
  ],
})
