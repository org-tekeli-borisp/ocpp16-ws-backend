import { test, expect } from '@playwright/test';

test.describe('Station Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('loads the application and shows sidebar', async ({ page }) => {
    await expect(page.locator('h1')).toBeVisible();
    await expect(page.locator('.sidebar')).toBeVisible();
    await expect(page.locator('.sidebar-header h2')).toBeVisible();
  });

  test('shows tab bar with four tabs', async ({ page }) => {
    const tabs = page.locator('.tabs .tab');
    await expect(tabs).toHaveCount(4);
  });

  test('shows select station hint when no station selected', async ({ page }) => {
    await expect(page.locator('.no-selection')).toBeVisible();
  });

  test('shows empty sidebar when no stations connected', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );
    await page.reload();
    await expect(page.locator('.sidebar .empty-state')).toBeVisible();
    await expect(page.locator('.no-selection')).toBeVisible();
  });

  test('switching tabs changes active tab', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );
    await page.reload();
    const tabs = page.locator('.tabs .tab');
    await expect(tabs).toHaveCount(4);

    await tabs.nth(0).click();
    await expect(tabs.nth(0)).toHaveClass(/active/);

    await tabs.nth(1).click();
    await expect(tabs.nth(1)).toHaveClass(/active/);

    await tabs.nth(2).click();
    await expect(tabs.nth(2)).toHaveClass(/active/);

    await tabs.nth(3).click();
    await expect(tabs.nth(3)).toHaveClass(/active/);
  });

  test('search box is visible in sidebar', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );
    await page.reload();
    const searchBox = page.locator('.search-box');
    await expect(searchBox).toBeVisible();

    await searchBox.fill('NONEXISTENT');
    await expect(page.locator('.sidebar .empty-state')).toBeVisible();

    await searchBox.fill('');
    await expect(page.locator('.sidebar .empty-state')).toBeVisible();
  });

  test('language selector switches language', async ({ page }) => {
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('en');

    await expect(page.locator('h1')).toHaveText('OCPP Central System');
  });
});
