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

const mockCps = [mockCp];
const mockCommands = ['reset', 'clear-cache', 'unlock-connector', 'trigger-message'];

function fulfillJson(route: import('@playwright/test').Route, data: unknown) {
  route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(data),
  });
}

async function setupRoutes(page: Page) {
  await page.route('**/api/chargepoints', (route) => fulfillJson(route, mockCps));
  await page.route('**/api/chargepoints/CP-001', (route) => fulfillJson(route, mockCp));
  await page.route('**/api/chargepoints/CP-001/commands*', (route) => fulfillJson(route, mockCommands));
}

test.describe('Deep Linking', () => {
  test.beforeEach(async ({ page }) => {
    await setupRoutes(page);
  });

  test('?cp=CP-001 auto-selects station on load', async ({ page }) => {
    await page.goto('/?cp=CP-001');
    await expect(page.locator('.station-badge')).toContainText('CP-001');
    await expect(page.locator('.no-selection')).not.toBeVisible();
  });

  test('?cp=CP-001 updates URL with hash after load', async ({ page }) => {
    await page.goto('/?cp=CP-001');
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);
    const url = new URL(page.url());
    expect(url.searchParams.get('cp')).toBe('CP-001');
  });

  test('?cp=CP-001#commands loads commands tab', async ({ page }) => {
    await page.goto('/?cp=CP-001#commands');
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);

    const activeTab = page.locator('.tabs .tab.active');
    await expect(activeTab).toContainText(/Commands|Commands/i);
  });

  test('?cp=CP-001#messages loads messages tab', async ({ page }) => {
    await page.goto('/?cp=CP-001#messages');
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);

    const activeTab = page.locator('.tabs .tab.active');
    await expect(activeTab).toContainText('Messages');
  });

  test('?cp=CP-001#transactions loads transactions tab', async ({ page }) => {
    await page.goto('/?cp=CP-001#transactions');
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);

    const activeTab = page.locator('.tabs .tab.active');
    await expect(activeTab).toContainText(/Sessions|Ladevorgänge/i);
  });

  test('#overview without ?cp shows no selection', async ({ page }) => {
    await page.goto('/#overview');
    await expect(page.locator('.no-selection')).toBeVisible();
    await expect(page.locator('.station-badge')).not.toBeVisible();
  });

  test('#commands without ?cp shows commands tab empty', async ({ page }) => {
    await page.goto('/#commands');
    const activeTab = page.locator('.tabs .tab.active');
    await expect(activeTab).toContainText('Commands');
    await expect(page.locator('.no-selection')).toBeVisible();
  });

  test('?cp=UNKNOWN with no matching station does not crash', async ({ page }) => {
    await page.route('**/api/chargepoints/UNKNOWN', (route) =>
      route.fulfill({ status: 404, body: 'Not Found' }),
    );

    await page.goto('/?cp=UNKNOWN');
    await page.waitForTimeout(1000);

    await expect(page.locator('.error-state')).toBeVisible();
  });

  test('clicking station updates URL with ?cp and #hash', async ({ page }) => {
    await page.goto('/');
    await page.locator('.station-item').first().click();
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);

    const url = new URL(page.url());
    expect(url.searchParams.get('cp')).toBe('CP-001');
    expect(url.hash).toBe('#overview');
  });

  test('switching tab updates URL hash', async ({ page }) => {
    await page.goto('/?cp=CP-001');
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);

    await page.locator('.tab').filter({ hasText: /Commands/i }).click();
    await page.waitForTimeout(200);

    const url = new URL(page.url());
    expect(url.searchParams.get('cp')).toBe('CP-001');
    expect(url.hash).toBe('#commands');
  });

  test('URL reflects correct cp after selecting second station', async ({ page }) => {
    await page.goto('/');
    await page.locator('.station-item').first().click();
    await expect(page.locator('.station-badge')).toContainText('CP-001');
    await page.waitForTimeout(300);

    const firstUrl = new URL(page.url());
    expect(firstUrl.searchParams.get('cp')).toBe('CP-001');
  });
});
