import { test, expect } from '@playwright/test';

test.describe('Station Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/src/app.html');
  });

  test('loads the application and shows sidebar', async ({ page }) => {
    await expect(page.locator('h1')).toBeVisible();
    await expect(page.locator('.sidebar')).toBeVisible();
    await expect(page.locator('.sidebar-header h2')).toBeVisible();
  });

  test('shows tab bar with three tabs', async ({ page }) => {
    const tabs = page.locator('.tabs .tab');
    await expect(tabs).toHaveCount(3);
  });

  test('shows select station hint when no station selected', async ({ page }) => {
    await expect(page.locator('.no-selection')).toBeVisible();
  });

  test('selecting a station shows overview tab', async ({ page }) => {
    const stationItem = page.locator('.station-item').first();
    await expect(stationItem).toBeVisible({ timeout: 10000 });
    await stationItem.click();

    await expect(page.locator('.panel')).toBeVisible();
    await expect(page.locator('.station-badge')).toBeVisible();
  });

  test('switching tabs changes content', async ({ page }) => {
    const stationItem = page.locator('.station-item').first();
    await expect(stationItem).toBeVisible({ timeout: 10000 });
    await stationItem.click();

    const tabs = page.locator('.tabs .tab');

    await tabs.nth(1).click();
    await expect(page.locator('#cmdSelect')).toBeVisible();

    await tabs.nth(2).click();
    await expect(page.locator('.message-list')).toBeVisible();

    await tabs.nth(0).click();
    await expect(page.locator('.overview-grid')).toBeVisible();
  });

  test('search filters station list', async ({ page }) => {
    const searchBox = page.locator('.search-box');
    await searchBox.fill('NONEXISTENT');

    await expect(page.locator('.empty-state')).toBeVisible();

    await searchBox.fill('');
    await expect(page.locator('.station-item')).toHaveCount({ count: 1, timeout: 10000 });
  });

  test('language selector switches language', async ({ page }) => {
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('en');

    await expect(page.locator('h1')).toHaveText('OCPP Central System');
  });
});
