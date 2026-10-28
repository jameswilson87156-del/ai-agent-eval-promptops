import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'http://127.0.0.1:18083').replace(/\/$/, '');
const failures = new Counter('business_workflow_failures');
const successRate = new Rate('business_workflow_success');
const runId = `k6-${new Date().toISOString().replace(/[:.]/g, '-')}-${Math.random().toString(16).slice(2, 10)}`;
const summaryFile = `/evidence/business-load-${runId}.json`;

const scenarioPlans = {
  low_rate: {
    targetWorkflowsPerSecond: 5,
    durationSeconds: 60,
    minimumIterations: 300,
    options: {
      executor: 'constant-arrival-rate',
      exec: 'businessWorkflow',
      rate: 5,
      timeUnit: '1s',
      duration: '60s',
      startTime: '0s',
      preAllocatedVUs: 10,
      maxVUs: 80,
      gracefulStop: '10s',
    },
  },
  medium_rate: {
    targetWorkflowsPerSecond: 10,
    durationSeconds: 60,
    minimumIterations: 600,
    options: {
      executor: 'constant-arrival-rate',
      exec: 'businessWorkflow',
      rate: 10,
      timeUnit: '1s',
      duration: '60s',
      startTime: '70s',
      preAllocatedVUs: 20,
      maxVUs: 80,
      gracefulStop: '10s',
    },
  },
  high_rate: {
    targetWorkflowsPerSecond: 20,
    durationSeconds: 60,
    minimumIterations: 1200,
    options: {
      executor: 'constant-arrival-rate',
      exec: 'businessWorkflow',
      rate: 20,
      timeUnit: '1s',
      duration: '60s',
      startTime: '140s',
      preAllocatedVUs: 40,
      maxVUs: 80,
      gracefulStop: '15s',
    },
  },
  stress_rate: {
    targetWorkflowsPerSecond: 100,
    durationSeconds: 60,
    minimumIterations: 6000,
    options: {
      executor: 'constant-arrival-rate',
      exec: 'businessWorkflow',
      rate: 100,
      timeUnit: '1s',
      duration: '60s',
      startTime: '210s',
      preAllocatedVUs: 80,
      maxVUs: 80,
      gracefulStop: '20s',
    },
  },
};

const perScenarioThresholds = Object.fromEntries(
  Object.entries(scenarioPlans).flatMap(([name, plan]) => [
    [`checks{scenario:${name}}`, ['rate==1']],
    [`http_req_failed{scenario:${name}}`, ['rate==0']],
    [`business_workflow_success{scenario:${name}}`, ['rate==1']],
    [`business_workflow_failures{scenario:${name}}`, ['count==0']],
    [`dropped_iterations{scenario:${name}}`, ['count==0']],
    [`iterations{scenario:${name}}`, [`count>=${plan.minimumIterations}`]],
  ]),
);

export const options = {
  scenarios: Object.fromEntries(
    Object.entries(scenarioPlans).map(([name, plan]) => [name, plan.options]),
  ),
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
    business_workflow_success: ['rate==1'],
    business_workflow_failures: ['count==0'],
    dropped_iterations: ['count==0'],
    ...perScenarioThresholds,
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

const customerId = 'synthetic-load-customer';
const requestTimeout = '10s';

function headers(idempotencyKey) {
  return {
    'Content-Type': 'application/json',
    'X-Demo-Actor-Id': customerId,
    'X-Demo-Role': 'customer',
    'Idempotency-Key': idempotencyKey,
  };
}

function fail(state, step, response) {
  state.failed = true;
  failures.add(1, { step });
  successRate.add(false);
  return check(response, { [`${step} returned expected HTTP status`]: () => false });
}

export function businessWorkflow() {
  const workflowId = `${runId}-${__VU}-${__ITER}-${Date.now()}`;
  const state = { failed: false };
  let orderId = '';

  group('create, pay, read, refund, read back', () => {
    const created = http.post(
      `${baseUrl}/api/business/orders`,
      JSON.stringify({ amount: 99.00, currency: 'CNY', itemName: 'Synthetic load-test order' }),
      { headers: headers(`load-create-${workflowId}`), timeout: requestTimeout, tags: { operation: 'order_create' } },
    );
    if (!check(created, {
      'order created': (response) => response.status === 201,
      'create returned pending order': (response) => {
        try { return response.status === 201 && response.json('status') === 'PENDING_PAYMENT'; } catch (_) { return false; }
      },
    })) return fail(state, 'order_create', created);
    try { orderId = created.json('orderId'); } catch (_) { return fail(state, 'order_create_body', created); }
    if (!orderId) return fail(state, 'order_create_id', created);

    const paid = http.post(
      `${baseUrl}/api/business/orders/${encodeURIComponent(orderId)}/payments`,
      JSON.stringify({ amount: 99.00 }),
      { headers: headers(`load-payment-${workflowId}`), timeout: requestTimeout, tags: { name: 'order_payment_create', operation: 'payment_create' } },
    );
    if (!check(paid, {
      'payment accepted': (response) => response.status === 201,
      'payment response is paid': (response) => {
        try { return response.status === 201 && response.json('order.status') === 'PAID'; } catch (_) { return false; }
      },
    })) return fail(state, 'payment_create', paid);

    const readPaid = http.get(`${baseUrl}/api/business/orders/${encodeURIComponent(orderId)}`, {
      headers: headers(`load-read-paid-${workflowId}`), timeout: requestTimeout, tags: { name: 'order_read_paid', operation: 'order_read' },
    });
    if (!check(readPaid, {
      'paid order can be retrieved': (response) => response.status === 200,
      'retrieved status is paid': (response) => {
        try { return response.status === 200 && response.json('status') === 'PAID'; } catch (_) { return false; }
      },
    })) return fail(state, 'order_read_paid', readPaid);

    const refunded = http.post(
      `${baseUrl}/api/business/orders/${encodeURIComponent(orderId)}/refunds`,
      JSON.stringify({ amount: 99.00, reason: 'Synthetic load-test refund' }),
      { headers: headers(`load-refund-${workflowId}`), timeout: requestTimeout, tags: { name: 'order_refund_create', operation: 'refund_create' } },
    );
    if (!check(refunded, {
      'refund accepted': (response) => response.status === 201,
      'refund response is complete': (response) => {
        try { return response.status === 201 && response.json('order.status') === 'REFUNDED'; } catch (_) { return false; }
      },
    })) return fail(state, 'refund_create', refunded);

    const readRefunded = http.get(`${baseUrl}/api/business/orders/${encodeURIComponent(orderId)}`, {
      headers: headers(`load-read-refunded-${workflowId}`), timeout: requestTimeout, tags: { name: 'order_read_refunded', operation: 'order_read' },
    });
    if (!check(readRefunded, {
      'refunded order can be retrieved': (response) => response.status === 200,
      'retrieved status is refunded': (response) => {
        try { return response.status === 200 && response.json('status') === 'REFUNDED'; } catch (_) { return false; }
      },
    })) return fail(state, 'order_read_refunded', readRefunded);
  });

  if (state.failed || !orderId) return;
  successRate.add(true);
}

export function handleSummary(data) {
  data.project_metadata = {
    run_id: runId,
    run_mode: 'local-synthetic-load',
    target: 'PromptOps isolated synthetic order/refund API',
    scenario_plans: Object.fromEntries(
      Object.entries(scenarioPlans).map(([name, plan]) => [name, {
        target_workflows_per_second: plan.targetWorkflowsPerSecond,
        duration_seconds: plan.durationSeconds,
        minimum_iterations: plan.minimumIterations,
      }]),
    ),
  };
  return {
    stdout: `k6 summary saved to ${summaryFile}\n`,
    [summaryFile]: JSON.stringify(data, null, 2),
  };
}
