import { test, expect, type Page } from '@playwright/test';

const mockCp = {
  chargePointId: 'CP-001',
  vendor: 'Tesla',
  model: 'Destination Charger',
  firmwareVersion: '1.2.3',
  status: 'ONLINE' as const,
  connectors: [
    { connectorId: 1, status: 'Available' as const },
    { connectorId: 2, status: 'Charging' as const },
  ],
  createdAt: '2025-01-15T10:30:00Z',
  lastSeenAt: new Date(Date.now() - 30_000).toISOString(),
};

const mockCpEmpty = {
  chargePointId: 'CP-003',
  vendor: '',
  model: '',
  firmwareVersion: undefined,
  status: 'ONLINE' as const,
  connectors: [],
  createdAt: '2025-03-01T12:00:00Z',
  lastSeenAt: new Date().toISOString(),
};

const mockCommands = ['reset', 'clear-cache', 'unlock-connector', 'trigger-message'];

function fulfillJson(route: import('@playwright/test').Route, data: unknown) {
  route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(data),
  });
}

async function setupRoutes(page: Page) {
  await page.route('**/api/chargepoints', (route) => fulfillJson(route, [mockCp, mockCpEmpty]));
  await page.route('**/api/chargepoints/CP-001', (route) => fulfillJson(route, mockCp));
  await page.route('**/api/chargepoints/CP-003', (route) => fulfillJson(route, mockCpEmpty));
  await page.route('**/api/chargepoints/CP-001/commands*', (route) => fulfillJson(route, mockCommands));
  await page.route('**/api/chargepoints/CP-003/commands*', (route) => fulfillJson(route, mockCommands));
}

test.describe('Overview Panel', () => {
  test.beforeEach(async ({ page }) => {
    await setupRoutes(page);
    await page.goto('/');
    await page.locator('.station-item').first().click();
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);
  });

  test('displays station chargePointId', async ({ page }) => {
    await expect(page.locator('.panel h2')).toContainText('CP-001');
  });

  test('displays online status indicator', async ({ page }) => {
    await expect(page.locator('.info-value .status-dot.online')).toBeVisible();
  });

  test('displays vendor and model', async ({ page }) => {
    await expect(page.locator('.panel-body')).toContainText('Tesla');
    await expect(page.locator('.panel-body')).toContainText('Destination Charger');
  });

  test('displays firmware version', async ({ page }) => {
    await expect(page.locator('.panel-body')).toContainText('1.2.3');
  });

  test('displays connector chips', async ({ page }) => {
    const chips = page.locator('.connector-chip');
    await expect(chips).toHaveCount(2);
    await expect(chips.first()).toContainText('#1');
    await expect(chips.first()).toContainText('Available');
    await expect(chips.last()).toContainText('#2');
    await expect(chips.last()).toContainText('Charging');
  });

  test('connector chip has correct CSS class for Available', async ({ page }) => {
    await expect(page.locator('.connector-chip.avail')).toBeVisible();
  });

  test('connector chip has correct CSS class for Charging', async ({ page }) => {
    await expect(page.locator('.connector-chip.charg')).toBeVisible();
  });
});

test.describe('Overview Panel — Empty Data', () => {
  test.beforeEach(async ({ page }) => {
    await setupRoutes(page);
    await page.goto('/');
    await page.locator('.station-item').last().click();
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);
  });

  test('shows no connector chips', async ({ page }) => {
    await expect(page.locator('.connector-chip')).toHaveCount(0);
  });
});
