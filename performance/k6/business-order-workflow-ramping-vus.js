import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'http://backend:18083').replace(/\/$/, '');
const failures = new Counter('business_workflow_failures');
const successRate = new Rate('business_workflow_success');
const runId = `k6-vu-${new Date().toISOString().replace(/[:.]/g, '-')}-${Math.random().toString(16).slice(2, 10)}`;
const summaryFile = `/evidence/business-vu-load-${runId}.json`;

const stagePlan = [
  { duration: '20s', target: 10, label: 'ramp to 10 VUs' },
  { duration: '30s', target: 10, label: 'hold 10 VUs' },
  { duration: '20s', target: 25, label: 'ramp to 25 VUs' },
  { duration: '30s', target: 25, label: 'hold 25 VUs' },
  { duration: '20s', target: 50, label: 'ramp to 50 VUs' },
  { duration: '30s', target: 50, label: 'hold 50 VUs' },
  { duration: '15s', target: 0, label: 'ramp down' },
];

export const options = {
  scenarios: {
    business_workflow_vus: {
      executor: 'ramping-vus',
      exec: 'pacedBusinessWorkflow',
      startVUs: 1,
      stages: stagePlan.map(({ duration, target }) => ({ duration, target })),
      gracefulRampDown: '10s',
      gracefulStop: '20s',
    },
  },
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
    business_workflow_success: ['rate==1'],
    business_workflow_failures: ['count==0'],
    dropped_iterations: ['count==0'],
    iterations: ['count>=3500'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

const customerId = 'synthetic-vu-load-customer';
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
      JSON.stringify({ amount: 99.00, currency: 'CNY', itemName: 'Synthetic concurrent-test order' }),
      { headers: headers(`vu-create-${workflowId}`), timeout: requestTimeout, tags: { operation: 'order_create' } },
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
      { headers: headers(`vu-payment-${workflowId}`), timeout: requestTimeout, tags: { name: 'order_payment_create', operation: 'payment_create' } },
    );
    if (!check(paid, {
      'payment accepted': (response) => response.status === 201,
      'payment response is paid': (response) => {
        try { return response.status === 201 && response.json('order.status') === 'PAID'; } catch (_) { return false; }
      },
    })) return fail(state, 'payment_create', paid);

    const readPaid = http.get(`${baseUrl}/api/business/orders/${encodeURIComponent(orderId)}`, {
      headers: headers(`vu-read-paid-${workflowId}`), timeout: requestTimeout, tags: { name: 'order_read_paid', operation: 'order_read' },
    });
    if (!check(readPaid, {
      'paid order can be retrieved': (response) => response.status === 200,
      'retrieved status is paid': (response) => {
        try { return response.status === 200 && response.json('status') === 'PAID'; } catch (_) { return false; }
      },
    })) return fail(state, 'order_read_paid', readPaid);

    const refunded = http.post(
      `${baseUrl}/api/business/orders/${encodeURIComponent(orderId)}/refunds`,
      JSON.stringify({ amount: 99.00, reason: 'Synthetic concurrent-test refund' }),
      { headers: headers(`vu-refund-${workflowId}`), timeout: requestTimeout, tags: { name: 'order_refund_create', operation: 'refund_create' } },
    );
    if (!check(refunded, {
      'refund accepted': (response) => response.status === 201,
      'refund response is complete': (response) => {
        try { return response.status === 201 && response.json('order.status') === 'REFUNDED'; } catch (_) { return false; }
      },
    })) return fail(state, 'refund_create', refunded);

    const readRefunded = http.get(`${baseUrl}/api/business/orders/${encodeURIComponent(orderId)}`, {
      headers: headers(`vu-read-refunded-${workflowId}`), timeout: requestTimeout, tags: { name: 'order_read_refunded', operation: 'order_read' },
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

export function pacedBusinessWorkflow() {
  try {
    businessWorkflow();
  } finally {
    // Client think time between complete workflows; this does not delay API responses.
    sleep(1);
  }
}

export function handleSummary(data) {
  data.project_metadata = {
    run_id: runId,
    run_mode: 'local-synthetic-vu-stress',
    target: 'PromptOps isolated synthetic order/refund API',
    executor: 'ramping-vus',
    target_active_vus: 50,
    stage_plan: stagePlan,
    client_think_time_seconds_between_workflows: 1,
    workflow: 'create order -> pay -> read -> refund -> read back',
    boundary: 'local Docker/H2 only; not production traffic or capacity evidence',
  };
  return {
    stdout: `k6 VU stress summary saved to ${summaryFile}\n`,
    [summaryFile]: JSON.stringify(data, null, 2),
  };
}
