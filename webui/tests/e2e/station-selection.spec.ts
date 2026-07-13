import { test, expect } from '@playwright/test';

const mockCps = [
  {
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
  },
  {
    chargePointId: 'CP-002',
    vendor: 'ABB',
    model: 'Terra 184',
    firmwareVersion: '3.7.1',
    status: 'OFFLINE' as const,
    connectors: [{ connectorId: 1, status: 'Unavailable' as const }],
    createdAt: '2025-02-20T08:00:00Z',
    lastSeenAt: '2025-01-01T00:00:00Z',
  },
];

const mockCommands = [
  'reset',
  'clear-cache',
  'unlock-connector',
  'remote-start-transaction',
  'trigger-message',
  'get-configuration',
];

function fulfillJson(route: import('@playwright/test').Route, data: unknown) {
  route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(data),
  });
}

async function setupRoutes(page: import('@playwright/test').Page) {
  await page.route('**/api/chargepoints', (route) => fulfillJson(route, mockCps));
  await page.route('**/api/chargepoints/CP-001', (route) => fulfillJson(route, mockCps[0]));
  await page.route('**/api/chargepoints/CP-002', (route) => fulfillJson(route, mockCps[1]));
  await page.route('**/api/chargepoints/CP-001/commands*', (route) => fulfillJson(route, mockCommands));
  await page.route('**/api/chargepoints/CP-002/commands*', (route) => fulfillJson(route, mockCommands));
}

test.describe('Station Selection', () => {
  test.beforeEach(async ({ page }) => {
    await setupRoutes(page);
    await page.goto('/');
    await expect(page.locator('h1')).toBeVisible();
  });

  test('clicking station selects it and shows overview', async ({ page }) => {
    await page.locator('.station-item').first().click();
    await expect(page.locator('.station-badge')).toContainText('CP-001');
    await expect(page.locator('.no-selection')).not.toBeVisible();
  });

  test('selecting station updates URL with cp param', async ({ page }) => {
    await page.locator('.station-item').first().click();
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);
    const url = new URL(page.url());
    expect(url.searchParams.get('cp')).toBe('CP-001');
    expect(url.hash).toBe('#overview');
  });

  test('sidebar shows station count and online/offline stats', async ({ page }) => {
    await expect(page.locator('.online-c')).toBeVisible();
    await expect(page.locator('.offline-c')).toBeVisible();
  });

  test('search filters stations by chargePointId', async ({ page }) => {
    await page.locator('.search-box').fill('CP-001');
    await expect(page.locator('.station-item')).toHaveCount(1);
    await expect(page.locator('.station-name')).toContainText('CP-001');
  });

  test('search filters stations by vendor', async ({ page }) => {
    await page.locator('.search-box').fill('ABB');
    await expect(page.locator('.station-item')).toHaveCount(1);
    await expect(page.locator('.station-name')).toContainText('CP-002');
  });

  test('empty search result shows empty state', async ({ page }) => {
    await page.locator('.search-box').fill('NONEXISTENT');
    await expect(page.locator('.sidebar .empty-state')).toBeVisible();
  });

  test('clicking offline station selects it', async ({ page }) => {
    await page.locator('.station-item').last().click();
    await expect(page.locator('.station-badge')).toContainText('CP-002');
  });
});
