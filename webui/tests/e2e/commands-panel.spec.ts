import { test, expect, type Page, type Route } from '@playwright/test';

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
const mockCommands = ['reset', 'clear-cache', 'unlock-connector', 'remote-start-transaction', 'trigger-message'];

async function setupRoutes(
  page: Page,
  cmdResponse: Record<string, unknown> = { status: 'Accepted' },
) {
  await page.route('**/api/**', (route: Route) => {
    const url = route.request().url();

    if (url.includes('/api/chargepoints/CP-001/commands/reset')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(cmdResponse),
      });
    }

    if (url.includes('/api/chargepoints/CP-001/commands/unlock-connector')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ status: 'Unlocked' }),
      });
    }

    if (url.includes('/api/chargepoints/CP-001/commands/clear-cache')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(cmdResponse),
      });
    }

    if (url.includes('/api/chargepoints/CP-001/commands')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockCommands),
      });
    }

    if (url.includes('/api/chargepoints/CP-001')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockCp),
      });
    }

    if (url.includes('/api/chargepoints')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockCps),
      });
    }

    route.continue();
  });
}

const cmdSelector = '#command-select';

async function selectStationAndGoToCommands(page: Page) {
  await page.locator('.station-item').first().click();
  await expect(page.locator('.station-badge')).toBeVisible();
  await page.locator('.tab').filter({ hasText: 'Commands' }).click();
  // Wait for commands API to populate the select
  await expect(page.locator(`${cmdSelector} option`)).toHaveCount(6);
}

test.describe('Commands Panel', () => {
  test.beforeEach(async ({ page }) => {
    await setupRoutes(page);
    await page.goto('/');
    await selectStationAndGoToCommands(page);
  });

  test('shows connector chips above command selector', async ({ page }) => {
    await expect(page.locator('.connector-chip')).toHaveCount(2);
  });

  test('shows command selector with options', async ({ page }) => {
    await expect(page.locator(`${cmdSelector} option`)).toHaveCount(6);
  });

  test('selecting command with no params shows hint', async ({ page }) => {
    await page.locator(cmdSelector).selectOption('clear-cache');
    await expect(page.locator('[style*="font-size:.85rem"]')).toContainText('no parameters');
  });

  test('selecting command with params shows form fields', async ({ page }) => {
    await page.locator(cmdSelector).selectOption('unlock-connector');
    await expect(page.locator('#f_connectorId')).toBeVisible();
  });

  test('send button is disabled without selected command', async ({ page }) => {
    await expect(page.locator('.btn.btn-primary')).toBeDisabled();
  });

  test('send button is enabled after selecting command', async ({ page }) => {
    await page.locator(cmdSelector).selectOption('reset');
    await expect(page.locator('.btn.btn-primary')).toBeEnabled();
  });

  test('sending command with no params shows response', async ({ page }) => {
    await page.locator(cmdSelector).selectOption('clear-cache');
    await page.locator('.btn.btn-primary').click();
    await expect(page.locator('.response-area')).toContainText('200');
    await expect(page.locator('.response-area')).toContainText('Accepted');
  });

  test('sending command with params passes payload', async ({ page }) => {
    let receivedBody: string | null = null;
    await page.route('**/api/**', async (route) => {
      if (route.request().url().includes('/commands/unlock-connector')) {
        receivedBody = route.request().postData();
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ status: 'Unlocked' }),
        });
      }
      // Fallback: handle via original setup
      const url = route.request().url();
      if (url.includes('/commands')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCommands),
        });
      }
      if (url.includes('/CP-001')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCp),
        });
      }
      if (url.includes('/chargepoints')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCps),
        });
      }
      route.continue();
    });

    await page.goto('/');
    await selectStationAndGoToCommands(page);
    await page.locator(cmdSelector).selectOption('unlock-connector');
    await page.locator('#f_connectorId').fill('1');
    await page.locator('.btn.btn-primary').click();
    await expect(page.locator('.response-area')).toContainText('200');

    expect(receivedBody).toBeTruthy();
    expect(JSON.parse(receivedBody!)).toEqual({ connectorId: 1 });
  });

  test('error response is displayed', async ({ page }) => {
    await page.route('**/api/**', (route) => {
      const url = route.request().url();
      if (url.includes('/commands/reset')) {
        return route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ errorCode: 'NotImplemented' }),
        });
      }
      if (url.includes('/commands')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCommands),
        });
      }
      if (url.includes('/CP-001')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCp),
        });
      }
      if (url.includes('/chargepoints')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCps),
        });
      }
      route.continue();
    });

    await page.goto('/');
    await selectStationAndGoToCommands(page);
    await page.locator(cmdSelector).selectOption('reset');
    await page.locator('.btn.btn-primary').click();
    await expect(page.locator('.response-area')).toContainText('500');
  });

  test('response info shows cpId and command name', async ({ page }) => {
    await page.locator(cmdSelector).selectOption('reset');
    await page.locator('.btn.btn-primary').click();
    await expect(page.locator('.response-info')).toContainText('CP-001');
    await expect(page.locator('.response-info')).toContainText('reset');
  });
});

test.describe('Commands Panel — Form Fields', () => {
  test.beforeEach(async ({ page }) => {
    await setupRoutes(page);
    await page.goto('/');
    await selectStationAndGoToCommands(page);
  });

  test('reset command shows type select with Hard/Soft options', async ({ page }) => {
    await page.locator(cmdSelector).selectOption('reset');
    await expect(page.locator('#f_type')).toBeVisible();
    await expect(page.locator('#f_type option')).toHaveCount(3);
  });

  test('remote-start-transaction shows multiple fields', async ({ page }) => {
    await page.locator(cmdSelector).selectOption('remote-start-transaction');
    await expect(page.locator('#f_idTag')).toBeVisible();
    await expect(page.locator('#f_connectorId')).toBeVisible();
  });
});
