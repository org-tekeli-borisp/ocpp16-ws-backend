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
const mockCommands = ['reset', 'clear-cache', 'trigger-message'];

const mockMessages = [
  {
    timestamp: '2025-07-12T14:30:00Z',
    direction: 'INBOUND' as const,
    messageType: 'Call',
    action: 'Heartbeat',
    payload: JSON.stringify({ timestamp: '2025-07-12T14:30:00Z' }),
  },
  {
    timestamp: '2025-07-12T14:31:00Z',
    direction: 'OUTBOUND' as const,
    messageType: 'CallResult',
    action: 'Heartbeat',
    payload: JSON.stringify({ currentTime: '2025-07-12T14:31:00Z' }),
  },
  {
    timestamp: '2025-07-12T14:32:00Z',
    direction: 'INBOUND' as const,
    messageType: 'Call',
    action: 'StatusNotification',
    payload: JSON.stringify({ connectorId: 1, errorReason: 'None', status: 'Available' }),
  },
];

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
  await page.route('**/api/chargepoints/CP-001/messages*', (route) => {
    const url = new URL(route.request().url());
    const isHistory = url.pathname.includes('/history');
    const body = isHistory
      ? { total: mockMessages.length, offset: 0, limit: 200, messages: mockMessages }
      : mockMessages;
    fulfillJson(route, body);
  });
}

test.describe('Messages Panel', () => {
  test.beforeEach(async ({ page }) => {
    await setupRoutes(page);
    await page.goto('/');
    await page.locator('.station-item').first().click();
    await expect(page.locator('.station-badge')).toBeVisible();
    await page.waitForTimeout(500);
    await page.locator('.tab').filter({ hasText: 'Messages' }).click();
    await expect(page.locator('.msg-tab').first()).toBeVisible();
  });

  test('shows Live tab selected by default', async ({ page }) => {
    const liveTab = page.locator('.msg-tab').filter({ hasText: 'Live' });
    await expect(liveTab).toHaveClass(/active/);
  });

  test('switching to History tab loads messages', async ({ page }) => {
    await page.locator('.msg-tab').filter({ hasText: 'Historie' }).click();
    const historyTab = page.locator('.msg-tab').filter({ hasText: 'Historie' });
    await expect(historyTab).toHaveClass(/active/);
  });

  test('displays message list items', async ({ page }) => {
    await expect(page.locator('.message-item')).toHaveCount(3);
  });

  test('shows direction indicators correctly', async ({ page }) => {
    await expect(page.locator('.msg-direction.inbound')).toHaveCount(2);
    await expect(page.locator('.msg-direction.outbound')).toHaveCount(1);
  });

  test('shows action names in message items', async ({ page }) => {
    const actions = page.locator('.msg-action');
    const texts = await actions.allTextContents();
    expect(texts).toContain('Heartbeat');
    expect(texts).toContain('StatusNotification');
  });

  test('shows message count in status bar', async ({ page }) => {
    await expect(page.locator('.status-bar')).toContainText('3');
  });

  test('live indicator is visible on Live tab', async ({ page }) => {
    await expect(page.locator('.live-indicator')).toBeVisible();
  });

  test('live indicator is hidden on History tab', async ({ page }) => {
    await page.locator('.msg-tab').filter({ hasText: 'Historie' }).click();
    await expect(page.locator('.live-indicator')).toHaveCSS('display', 'none');
  });

  test('filter by direction works', async ({ page }) => {
    await page.locator('.filters select').selectOption('INBOUND');
    await page.locator('.filters .btn-outline').click();
    await expect(page.locator('.message-item')).toHaveCount(2);
  });

  test('filter by action works', async ({ page }) => {
    await page.locator('.filters input[type="text"]').fill('Heartbeat');
    await page.locator('.filters .btn-outline').click();
    await expect(page.locator('.message-item')).toHaveCount(2);
  });

  test('empty filter shows no messages state', async ({ page }) => {
    await page.locator('.filters input[type="text"]').fill('NONEXISTENT');
    await page.locator('.filters .btn-outline').click();
    await expect(page.locator('.message-list .empty-state')).toBeVisible();
  });
});
