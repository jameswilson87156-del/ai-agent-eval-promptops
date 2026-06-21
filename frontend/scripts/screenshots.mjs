import assert from 'node:assert/strict'
import { spawn, spawnSync } from 'node:child_process'
import { mkdir, stat } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const frontendDir = path.resolve(scriptDir, '..')
const repoRoot = path.resolve(frontendDir, '..')
const standardDir = path.join(repoRoot, 'docs', 'images')
const largeDir = path.join(standardDir, 'large')
const isWindows = process.platform === 'win32'
const backendPort = 18081
const frontendPort = 5175
const children = []
const screens = [
  ['dashboard', 'overview'],
  ['eval-run', 'run'],
  ['prompt-versions', 'versions'],
  ['trace-review', 'review'],
]

function startProcess(command, args, cwd, env = {}) {
  const executable = isWindows ? (process.env.ComSpec ?? 'cmd.exe') : command
  const executableArgs = isWindows ? ['/d', '/s', '/c', command, ...args] : args
  const child = spawn(executable, executableArgs, {
    cwd,
    env: { ...process.env, ...env },
    windowsHide: true,
    detached: !isWindows,
    shell: false,
    stdio: ['ignore', 'pipe', 'pipe'],
  })
  child.stdout.on('data', (chunk) => process.stdout.write(`[${path.basename(command)}] ${chunk}`))
  child.stderr.on('data', (chunk) => process.stderr.write(`[${path.basename(command)}] ${chunk}`))
  children.push(child)
  return child
}

async function waitFor(url, timeoutMs = 90_000) {
  const deadline = Date.now() + timeoutMs
  let lastError
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url)
      if (response.ok) return
      lastError = new Error(`${url} returned ${response.status}`)
    } catch (error) {
      lastError = error
    }
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  throw new Error(`Timed out waiting for ${url}: ${lastError?.message ?? 'unknown error'}`)
}

async function waitForUnavailable(url, timeoutMs = 15_000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    try {
      await fetch(url)
    } catch {
      return
    }
    await new Promise((resolve) => setTimeout(resolve, 250))
  }
  throw new Error(`Process cleanup did not release ${url}`)
}

function stopProcess(child) {
  if (!child?.pid || child.exitCode !== null) return
  if (isWindows) {
    spawnSync('taskkill', ['/PID', String(child.pid), '/T', '/F'], { windowsHide: true, stdio: 'ignore' })
  } else {
    try {
      process.kill(-child.pid, 'SIGTERM')
    } catch {
      child.kill('SIGTERM')
    }
  }
}

async function launchBrowser() {
  try {
    return await chromium.launch({ channel: 'chrome', headless: true })
  } catch {
    return chromium.launch({ headless: true })
  }
}

function monitorBrowserErrors(page, label) {
  const errors = []
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(`console: ${message.text()}`)
  })
  page.on('pageerror', (error) => errors.push(`pageerror: ${error.message}`))
  return () => assert.deepEqual(errors, [], `${label} emitted browser errors`)
}

async function openView(page, view) {
  const response = await page.goto(`http://127.0.0.1:${frontendPort}/?view=${view}`, { waitUntil: 'networkidle' })
  assert.ok(response?.ok(), `${view} did not return a successful document response`)
  await page.locator('[data-ready="true"]').waitFor({ state: 'visible', timeout: 30_000 })
  await page.evaluate(() => document.fonts.ready)
}

async function assertNoHorizontalOverflow(page, label) {
  const dimensions = await page.evaluate(() => ({
    viewport: window.innerWidth,
    document: document.documentElement.scrollWidth,
    body: document.body.scrollWidth,
  }))
  assert.ok(
    Math.max(dimensions.document, dimensions.body) <= dimensions.viewport + 1,
    `${label} overflows horizontally: ${JSON.stringify(dimensions)}`,
  )
}

async function runResponsiveAssertions(browser) {
  for (const viewport of [{ width: 1366, height: 900 }, { width: 390, height: 844 }]) {
    const context = await browser.newContext({ viewport, colorScheme: 'light', reducedMotion: 'reduce' })
    const page = await context.newPage()
    const assertNoBrowserErrors = monitorBrowserErrors(page, `${viewport.width}px responsive checks`)
    for (const [, view] of screens) {
      await openView(page, view)
      await assertNoHorizontalOverflow(page, `${view} at ${viewport.width}px`)
    }
    assertNoBrowserErrors()
    await context.close()
  }
}

async function runInteractionAssertions(browser) {
  const context = await browser.newContext({
    viewport: { width: 1366, height: 900 },
    colorScheme: 'light',
    reducedMotion: 'reduce',
  })
  const page = await context.newPage()
  const assertNoBrowserErrors = monitorBrowserErrors(page, 'interaction checks')
  await openView(page, 'overview')

  const promptButtons = page.locator('[data-prompt-id]')
  assert.ok(await promptButtons.count() >= 2, 'Prompt switch requires at least two Demo prompts')
  const secondPromptId = await promptButtons.nth(1).getAttribute('data-prompt-id')
  await promptButtons.nth(1).click()
  await page.waitForFunction(
    (id) => document.querySelector(`[data-prompt-id="${id}"]`)?.getAttribute('aria-pressed') === 'true',
    secondPromptId,
  )
  const firstPromptId = await promptButtons.nth(0).getAttribute('data-prompt-id')
  await promptButtons.nth(0).click()
  await page.waitForFunction(
    (id) => document.querySelector(`[data-prompt-id="${id}"]`)?.getAttribute('aria-pressed') === 'true',
    firstPromptId,
  )

  await page.locator('[data-view="run"]').click()
  const search = page.locator('[data-testid="global-search"]')
  await search.fill('no-such-prompt-run-case-trace')
  await page.locator('.empty-state').filter({ hasText: '没有匹配的 Case' }).waitFor({ state: 'visible' })
  await page.getByRole('button', { name: '清除搜索与筛选' }).click()
  assert.equal(await search.inputValue(), '', 'Clearing the empty state must retain a usable search input')

  await page.locator('[data-filter="risk"]').click()
  assert.ok(await page.locator('.case-table > button').count() >= 1, 'High-risk filter returned no Demo cases')
  await page.locator('[data-filter="all"]').click()
  const runCases = page.locator('.case-table > button')
  assert.ok(await runCases.count() >= 2, 'Case and Trace checks require at least two Demo cases')

  const pendingMetric = page.locator('[data-metric="当前待 Review"] strong')
  const pendingBefore = Number.parseInt((await pendingMetric.textContent()) ?? '', 10)
  await runCases.nth(0).click()
  const traceHeading = page.locator('.trace-detail-panel h2')
  await traceHeading.waitFor({ state: 'visible' })
  const firstTraceHeading = await traceHeading.textContent()
  await page.locator('.review-case-list button').nth(1).click()
  await page.waitForFunction(
    (heading) => document.querySelector('.trace-detail-panel h2')?.textContent !== heading,
    firstTraceHeading,
  )

  await page.locator('[data-review-action="approved"]').click()
  await page.locator('[data-testid="review-state"]').filter({ hasText: '本地标记：通过' }).waitFor({ state: 'visible' })
  const pendingAfter = Number.parseInt((await pendingMetric.textContent()) ?? '', 10)
  assert.equal(pendingAfter, pendingBefore - 1, 'Review action did not update the pending Review count')
  await page.locator('[data-filter="review"]').click()
  assert.equal(
    await page.locator('.review-case-list button[aria-pressed="true"]').count(),
    0,
    'Reviewed Case should leave the pending Review queue',
  )

  await assertNoHorizontalOverflow(page, 'interaction result at 1366px')
  assertNoBrowserErrors()
  await context.close()
}

async function assertScreenshotFiles() {
  for (const directory of [standardDir, largeDir]) {
    for (const [name] of screens) {
      const screenshotPath = path.join(directory, `${name}.png`)
      const metadata = await stat(screenshotPath)
      assert.ok(metadata.isFile() && metadata.size > 10_000, `${screenshotPath} is missing or unexpectedly small`)
    }
  }
}

async function captureSet(browser, outputDir, viewport) {
  const context = await browser.newContext({
    viewport,
    colorScheme: 'light',
    reducedMotion: 'reduce',
    deviceScaleFactor: 1,
  })
  const page = await context.newPage()
  const assertNoBrowserErrors = monitorBrowserErrors(page, `${viewport.width}px screenshots`)
  for (const [name, view] of screens) {
    await openView(page, view)
    await assertNoHorizontalOverflow(page, `${view} screenshot at ${viewport.width}px`)
    await page.screenshot({ path: path.join(outputDir, `${name}.png`), fullPage: true })
  }
  assertNoBrowserErrors()
  await context.close()
}

let browser
try {
  await mkdir(largeDir, { recursive: true })
  startProcess(isWindows ? 'mvn.cmd' : 'mvn', ['-pl', 'backend', 'spring-boot:run'], repoRoot, {
    SPRING_PROFILES_ACTIVE: 'demo',
    SERVER_PORT: String(backendPort),
  })
  startProcess(
    isWindows ? 'npm.cmd' : 'npm',
    ['run', 'dev', '--', '--host', '127.0.0.1', '--port', String(frontendPort), '--strictPort'],
    frontendDir,
    { VITE_API_PROXY_TARGET: `http://127.0.0.1:${backendPort}` },
  )

  await Promise.all([
    waitFor(`http://127.0.0.1:${backendPort}/api/dashboard/summary`),
    waitFor(`http://127.0.0.1:${frontendPort}`),
  ])

  browser = await launchBrowser()
  await runResponsiveAssertions(browser)
  await runInteractionAssertions(browser)
  await captureSet(browser, standardDir, { width: 1440, height: 1000 })
  await captureSet(browser, largeDir, { width: 1920, height: 1080 })
  await assertScreenshotFiles()
  process.stdout.write(`Captured ${screens.length * 2} screenshots in ${standardDir}\n`)
} finally {
  await browser?.close()
  children.reverse().forEach(stopProcess)
  await Promise.all([
    waitForUnavailable(`http://127.0.0.1:${backendPort}/api/dashboard/summary`),
    waitForUnavailable(`http://127.0.0.1:${frontendPort}`),
  ])
}
