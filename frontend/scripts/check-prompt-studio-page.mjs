import { checkShowcasePage } from './page-check-common.mjs'

await checkShowcasePage({
  slug: 'prompt-studio',
  view: 'prompt',
  frontendPort: 5180,
  shellSelector: '.prompt-shell',
  requiredSelectors: [
    '.prompt-sidebar',
    '.prompt-topbar',
    '.prompt-title',
    '.prompt-list',
    '.prompt-editor',
    '.prompt-score',
    '.prompt-runs',
    '.prompt-summary',
    '.prompt-suggestions',
    '.prompt-history',
    '.prompt-recent',
  ],
  countChecks: [
    { selector: '.prompt-list > button', expected: 5, label: 'Prompt Studio must show five prompt templates' },
    { selector: '.prompt-recent-row', expected: 4, label: 'Prompt Studio must show recent runs' },
  ],
})
