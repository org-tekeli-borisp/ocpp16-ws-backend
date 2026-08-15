import { test, expect, type Page } from '@playwright/test';

const mockCp = {
  chargePointId: 'CP-001',
  vendor: 'Tesla',
  model: 'Destination Charger',
  firmwareVersion: '1.2.3',
  status: 'ONLINE' as const,
  connectors: [{ connectorId: 1, status: 'Available' as const }],
  createdAt: '2025-01-15T10:30:00Z',
  lastSeenAt: new Date(Date.now() - 30_000).toISOString(),
};

const mockCommands = ['reset', 'clear-cache'];
const cmdSelector = '#command-select';

function fulfillJson(route: import('@playwright/test').Route, data: unknown) {
  route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(data),
  });
}

test.describe('Error States', () => {
  test('shows error banner when fetchChargePoints fails', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 500, body: 'Internal Server Error' }),
    );
    await page.goto('/');
    await expect(page.locator('.error-state')).toBeVisible();
  });

  test('shows error when fetchChargePoint fails on selection', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) => fulfillJson(route, [mockCp]));
    await page.route('**/api/chargepoints/CP-001', (route) =>
      route.fulfill({ status: 404, body: 'Not Found' }),
    );
    await page.goto('/');
    await page.locator('.station-item').first().click();
    await page.waitForTimeout(500);
    await expect(page.locator('.error-state')).toBeVisible();
  });

  test('shows network error in command response on fetch failure', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) => fulfillJson(route, [mockCp]));
    await page.route('**/api/chargepoints/CP-001', (route) => fulfillJson(route, mockCp));
    await page.route('**/api/chargepoints/CP-001/commands*', (route) => fulfillJson(route, mockCommands));
    await page.route('**/api/chargepoints/CP-001/commands/reset', (route) => route.abort());

    await page.goto('/');
    await page.locator('.station-item').first().click();
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);
    await page.locator('.tab').filter({ hasText: 'Commands' }).click();
    await page.locator(cmdSelector).selectOption('reset');
    await page.locator('.btn.btn-primary').click();
    await expect(page.locator('.response-area')).toContainText('error');
  });

  test('shows error for 404 on command endpoint', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) => fulfillJson(route, [mockCp]));
    await page.route('**/api/chargepoints/CP-001', (route) => fulfillJson(route, mockCp));
    await page.route('**/api/chargepoints/CP-001/commands*', (route) => {
      const url = new URL(route.request().url());
      if (url.pathname.endsWith('/commands/reset')) {
        return route.fulfill({ status: 404, contentType: 'application/json', body: 'null' });
      }
      fulfillJson(route, mockCommands);
    });

    await page.goto('/');
    await page.locator('.station-item').first().click();
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);
    await page.locator('.tab').filter({ hasText: 'Commands' }).click();
    await page.locator(cmdSelector).selectOption('reset');
    await page.locator('.btn.btn-primary').click();
    await expect(page.locator('.response-area')).toContainText('404');
  });

  test('shows empty state on messages error', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) => fulfillJson(route, [mockCp]));
    await page.route('**/api/chargepoints/CP-001', (route) => fulfillJson(route, mockCp));
    await page.route('**/api/chargepoints/CP-001/commands*', (route) => fulfillJson(route, mockCommands));
    await page.route('**/api/chargepoints/CP-001/messages*', (route) =>
      route.fulfill({ status: 500, body: 'Server Error' }),
    );

    await page.goto('/');
    await page.locator('.station-item').first().click();
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);
    await page.locator('.tab').filter({ hasText: 'Messages' }).click();
    await expect(page.locator('.message-list .empty-state')).toBeVisible();
  });
});
