import assert from 'node:assert/strict'
import { spawn, spawnSync } from 'node:child_process'
import { mkdir, stat, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const frontendDir = path.resolve(scriptDir, '..')
const repoRoot = path.resolve(frontendDir, '..')
const outputDir = path.join(repoRoot, '.local')
const outputPath = path.join(outputDir, 'promptops-dashboard-overview-16x9-check.png')
const formalOutputPath = path.join(repoRoot, 'docs', 'images', 'eval-dashboard.png')
const isWindows = process.platform === 'win32'
const backendPort = 18082
const frontendPort = 5176
const viewport = { width: 1440, height: 810 }
const children = []

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
    return
  }
  try {
    process.kill(-child.pid, 'SIGTERM')
  } catch {
    child.kill('SIGTERM')
  }
}

async function launchBrowser() {
  try {
    return await chromium.launch({ channel: 'chrome', headless: true })
  } catch {
    return chromium.launch({ headless: true })
  }
}

const requiredSelectors = [
  '.eval-topbar',
  '.eval-sidebar',
  '.dashboard-title-row',
  '.experiment-card',
  '.metrics-block',
  '.health-card',
  '.recent-runs-card',
  '.trace-master-panel',
  '.failure-card',
  '.provider-card',
  '.loop-card',
]

let browser
try {
  await Promise.all([
    mkdir(outputDir, { recursive: true }),
    mkdir(path.dirname(formalOutputPath), { recursive: true }),
  ])
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
  const context = await browser.newContext({
    viewport,
    colorScheme: 'light',
    reducedMotion: 'reduce',
    deviceScaleFactor: 1,
  })
  const page = await context.newPage()
  const browserErrors = []
  page.on('console', (message) => {
    if (message.type() === 'error') browserErrors.push(`console: ${message.text()}`)
  })
  page.on('pageerror', (error) => browserErrors.push(`pageerror: ${error.message}`))

  const response = await page.goto(`http://127.0.0.1:${frontendPort}/?view=overview`, { waitUntil: 'networkidle' })
  assert.ok(response?.ok(), 'Dashboard did not return a successful document response')
  await page.locator('[data-ready="true"]').waitFor({ state: 'visible', timeout: 30_000 })
  await page.evaluate(() => document.fonts.ready)

  const dimensions = await page.evaluate(() => ({
    viewportWidth: window.innerWidth,
    viewportHeight: window.innerHeight,
    documentWidth: document.documentElement.scrollWidth,
    documentHeight: document.documentElement.scrollHeight,
    bodyWidth: document.body.scrollWidth,
    bodyHeight: document.body.scrollHeight,
  }))
  assert.ok(
    Math.max(dimensions.documentWidth, dimensions.bodyWidth) <= viewport.width + 1,
    `Dashboard overflows horizontally: ${JSON.stringify(dimensions)}`,
  )
  assert.ok(
    Math.max(dimensions.documentHeight, dimensions.bodyHeight) <= viewport.height + 1,
    `Dashboard requires vertical scrolling: ${JSON.stringify(dimensions)}`,
  )

  for (const selector of requiredSelectors) {
    const locator = page.locator(selector)
    await locator.waitFor({ state: 'visible' })
    const box = await locator.boundingBox()
    assert.ok(box, `${selector} has no visible bounding box`)
    assert.ok(box.x >= -1 && box.y >= -1, `${selector} starts outside the viewport: ${JSON.stringify(box)}`)
    assert.ok(
      box.x + box.width <= viewport.width + 1 && box.y + box.height <= viewport.height + 1,
      `${selector} is clipped by the viewport: ${JSON.stringify(box)}`,
    )
  }

  const tracePanelBox = await page.locator('.trace-master-panel').boundingBox()
  assert.ok(tracePanelBox, 'Trace panel has no visible bounding box')
  const traceNodes = page.locator('.trace-node')
  assert.equal(await traceNodes.count(), 7, 'Trace must display all 7 stages')
  for (let index = 0; index < 7; index += 1) {
    const box = await traceNodes.nth(index).boundingBox()
    assert.ok(box, `Trace node ${index + 1} has no visible bounding box`)
    assert.ok(
      box.y >= tracePanelBox.y - 1 && box.y + box.height <= tracePanelBox.y + tracePanelBox.height + 1,
      `Trace node ${index + 1} is clipped by the Trace panel: ${JSON.stringify(box)}`,
    )
  }
  await traceNodes.filter({ hasText: 'Failure Case' }).waitFor({ state: 'visible' })
  await traceNodes.filter({ hasText: 'Human Review' }).waitFor({ state: 'visible' })

  assert.deepEqual(browserErrors, [], 'Dashboard emitted browser errors')
  const screenshot = await page.screenshot({ path: outputPath, fullPage: false })
  assert.equal(screenshot.readUInt32BE(16), viewport.width, 'Screenshot width is not 1440px')
  assert.equal(screenshot.readUInt32BE(20), viewport.height, 'Screenshot height is not 810px')
  await writeFile(formalOutputPath, screenshot)
  const [localMetadata, formalMetadata] = await Promise.all([stat(outputPath), stat(formalOutputPath)])
  assert.ok(localMetadata.size > 10_000, 'Dashboard screenshot is unexpectedly small')
  assert.equal(formalMetadata.size, localMetadata.size, 'Formal dashboard screenshot differs from checked screenshot')

  await context.close()
  process.stdout.write(`Captured ${viewport.width}x${viewport.height} dashboard screenshot at ${outputPath}\n`)
  process.stdout.write(`Wrote verified project screenshot to ${formalOutputPath}\n`)
} finally {
  await browser?.close()
  children.reverse().forEach(stopProcess)
  await Promise.all([
    waitForUnavailable(`http://127.0.0.1:${backendPort}/api/dashboard/summary`),
    waitForUnavailable(`http://127.0.0.1:${frontendPort}`),
  ])
}
