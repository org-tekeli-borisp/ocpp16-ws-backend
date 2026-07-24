import { test, expect, type Page } from '@playwright/test';

const mockCp = {
  chargePointId: 'CP-001',
  vendor: 'Tesla',
  model: 'Destination Charger',
  firmwareVersion: '1.2.3',
  status: 'ONLINE' as const,
  connectors: [
    { connectorId: 1, status: 'Charging' as const },
    { connectorId: 2, status: 'Available' as const },
  ],
  createdAt: '2025-01-15T10:30:00Z',
  lastSeenAt: new Date(Date.now() - 30_000).toISOString(),
};

const mockCps = [mockCp];
const mockCommands = ['reset', 'clear-cache'];

const activeTx = {
  id: 100,
  chargePointId: 'CP-001',
  connectorId: 1,
  idTag: 'CARD-A1',
  meterStart: 0,
  startTime: new Date(Date.now() - 300_000).toISOString(),
  stopTime: null,
  meterStop: null,
  stopReason: null,
  durationSeconds: null,
  energyWh: null,
};

const historyTx = [
  {
    id: 90,
    chargePointId: 'CP-001',
    connectorId: 1,
    idTag: 'CARD-B2',
    meterStart: 0,
    startTime: '2025-06-10T08:00:00Z',
    stopTime: '2025-06-10T09:30:00Z',
    meterStop: 50000,
    stopReason: 'Local',
    durationSeconds: 5400,
    energyWh: 45000,
  },
  {
    id: 91,
    chargePointId: 'CP-001',
    connectorId: 2,
    idTag: 'CARD-C3',
    meterStart: 1000,
    startTime: '2025-06-11T14:00:00Z',
    stopTime: '2025-06-11T14:45:00Z',
    meterStop: 56000,
    stopReason: 'Remote',
    durationSeconds: 2700,
    energyWh: 22000,
  },
];

function fulfillJson(route: import('@playwright/test').Route, data: unknown) {
  route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(data),
  });
}

async function setupRoutes(
  page: Page,
  active: typeof activeTx | null = activeTx,
  history: typeof historyTx = historyTx,
) {
  await page.route('**/api/**', (route) => {
    const url = route.request().url();

    if (url.includes('/api/chargepoints/CP-001/transactions')) {
      const params = new URL(url).searchParams;
      if (params.get('running') === 'true') {
        return fulfillJson(route, active ? [active] : []);
      }
      return fulfillJson(route, history);
    }

    if (url.includes('/commands') && !url.includes('/transactions')) {
      return fulfillJson(route, mockCommands);
    }

    if (url.includes('/CP-001')) {
      return fulfillJson(route, mockCp);
    }

    if (url.includes('/chargepoints')) {
      return fulfillJson(route, mockCps);
    }

    route.continue();
  });
}

async function selectStationAndGoToTransactions(page: Page) {
  await page.locator('.station-item').first().click();
  await expect(page.locator('.station-badge')).toBeVisible();
  await page.waitForTimeout(500);
  await page.locator('.tab').filter({ hasText: /Sessions|Ladevorgänge/i }).click();
  await expect(page.locator('.msg-tab')).toHaveCount(2);
}

test.describe('Transactions Panel', () => {
  test.beforeEach(async ({ page }) => {
    await setupRoutes(page);
    await page.goto('/');
    await selectStationAndGoToTransactions(page);
  });

  test('shows Active tab selected by default', async ({ page }) => {
    const activeTab = page.locator('.msg-tab').first();
    await expect(activeTab).toHaveClass(/active/);
  });

  test('shows active transaction row', async ({ page }) => {
    await expect(page.locator('.tx-row-active')).toHaveCount(1);
  });

  test('shows idTag in active transaction', async ({ page }) => {
    await expect(page.locator('.tx-row-active .tx-idtag')).toContainText('CARD-A1');
  });

  test('shows connector ID in active transaction', async ({ page }) => {
    await expect(page.locator('.tx-row-active .tx-conn')).toContainText('1');
  });

  test('shows running badge in active transaction', async ({ page }) => {
    await expect(page.locator('.tx-row-active .tx-running-badge')).toBeVisible();
  });

  test('shows live duration indicator in active transaction', async ({ page }) => {
    await expect(page.locator('.tx-row-active .tx-live .live-dot')).toBeVisible();
  });

  test('switching to History tab shows history rows', async ({ page }) => {
    await page.locator('.msg-tab').filter({ hasText: /History|Verlauf/i }).click();
    await expect(page.locator('.msg-table tbody tr')).toHaveCount(2);
  });

  test('history row shows all columns', async ({ page }) => {
    await page.locator('.msg-tab').filter({ hasText: /History|Verlauf/i }).click();
    const row = page.locator('.msg-table tbody tr').first();

    await expect(row.locator('.tx-conn')).toBeVisible();
    await expect(row.locator('.tx-idtag')).toBeVisible();
    await expect(row.locator('.tx-time')).toHaveCount(2);
    await expect(row.locator('.tx-energy')).toBeVisible();
    await expect(row.locator('.tx-duration')).toBeVisible();
    await expect(row.locator('.tx-reason')).toBeVisible();
  });

  test('history shows energy in kWh', async ({ page }) => {
    await page.locator('.msg-tab').filter({ hasText: /History|Verlauf/i }).click();
    await expect(page.locator('.msg-table .tx-energy').first()).toContainText('45.00 kWh');
  });

  test('history shows duration formatted', async ({ page }) => {
    await page.locator('.msg-tab').filter({ hasText: /History|Verlauf/i }).click();
    await expect(page.locator('.msg-table .tx-duration').first()).toContainText('1h 30m');
  });

  test('history shows stop reason', async ({ page }) => {
    await page.locator('.msg-tab').filter({ hasText: /History|Verlauf/i }).click();
    await expect(page.locator('.msg-table .tx-reason').first()).toContainText('Local');
  });

  test('connector filter dropdown is populated', async ({ page }) => {
    const options = page.locator('.tx-filters select option');
    await expect(options).toHaveCount(3);
  });

  test('filtering by connector 1 in history', async ({ page }) => {
    await page.locator('.msg-tab').filter({ hasText: /History|Verlauf/i }).click();
    await page.locator('.tx-filters select').selectOption('1');
    await expect(page.locator('.msg-table tbody tr')).toHaveCount(1);
    await expect(page.locator('.msg-table .tx-conn').first()).toContainText('1');
  });

  test('filtering by connector 2 in history', async ({ page }) => {
    await page.locator('.msg-tab').filter({ hasText: /History|Verlauf/i }).click();
    await page.locator('.tx-filters select').selectOption('2');
    await expect(page.locator('.msg-table tbody tr')).toHaveCount(1);
    await expect(page.locator('.msg-table .tx-conn').first()).toContainText('2');
  });

  test('refresh button is visible', async ({ page }) => {
    await expect(page.locator('.tx-filters .btn-outline')).toBeVisible();
  });
});

test.describe('Transactions Panel — Empty States', () => {
  test('shows empty state for no active transactions', async ({ page }) => {
    await setupRoutes(page, null, []);
    await page.goto('/');
    await selectStationAndGoToTransactions(page);
    await expect(page.locator('.panel .empty-state')).toBeVisible();
  });

  test('shows empty state for no history transactions', async ({ page }) => {
    await setupRoutes(page, null, []);
    await page.goto('/');
    await selectStationAndGoToTransactions(page);
    await page.locator('.msg-tab').filter({ hasText: /History|Verlauf/i }).click();
    await expect(page.locator('.panel .empty-state')).toBeVisible();
  });
});

test.describe('Transactions Panel — Error Handling', () => {
  test('shows empty state on transactions fetch error', async ({ page }) => {
    await page.route('**/api/chargepoints/CP-001/transactions*', (route) =>
      route.fulfill({ status: 500, body: 'Internal Error' }),
    );
    await page.route('**/api/chargepoints/CP-001', (route) => fulfillJson(route, mockCp));
    await page.route('**/api/chargepoints', (route) => fulfillJson(route, mockCps));
    await page.route('**/api/chargepoints/CP-001/commands*', (route) => fulfillJson(route, mockCommands));

    await page.goto('/');
    await page.locator('.station-item').first().click();
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);
  await page.locator('.tab').nth(3).click();
    await expect(page.locator('.panel .empty-state')).toBeVisible();
  });
});
