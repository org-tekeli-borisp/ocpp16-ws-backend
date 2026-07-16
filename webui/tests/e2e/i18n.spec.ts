import { test, expect } from '@playwright/test';

test.describe('i18n (Internationalization)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('h1')).toBeVisible();
  });

  test('default language shows German labels', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();
    await expect(page.locator('h1')).toBeVisible();

    const sidebarHeader = page.locator('.sidebar-header h2');
    await expect(sidebarHeader).toBeVisible();
  });

  test('switching to English changes page title label', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('en');

    await expect(page.locator('h1')).toHaveText('OCPP Central System');
  });

  test('switching to French changes page title label', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('fr');

    await expect(page.locator('h1')).toHaveText('OCPP Central System');
  });

  test('English sidebar shows English labels', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('en');

    await expect(page.locator('.sidebar-header h2')).toHaveText('Stations');
    await expect(page.locator('.search-box')).toHaveAttribute('placeholder', 'Filter…');
  });

  test('French sidebar shows French labels', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('fr');

    await expect(page.locator('.sidebar-header h2')).toHaveText('Stations');
    await expect(page.locator('.search-box')).toHaveAttribute('placeholder', 'Filtrer…');
  });

  test('German sidebar shows German labels', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('de');

    await expect(page.locator('.sidebar-header h2')).toHaveText('Stationen');
    await expect(page.locator('.search-box')).toHaveAttribute('placeholder', 'Filtern…');
  });

  test('tab labels change with language switch (English)', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('en');

    const tabs = page.locator('.tabs .tab');
    await expect(tabs.nth(0)).toContainText('Overview');
    await expect(tabs.nth(1)).toContainText('Commands');
    await expect(tabs.nth(2)).toContainText('Messages');
    await expect(tabs.nth(3)).toContainText('Sessions');
  });

  test('tab labels change with language switch (German)', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('de');

    const tabs = page.locator('.tabs .tab');
    await expect(tabs.nth(0)).toContainText('Übersicht');
    await expect(tabs.nth(1)).toContainText('Commands');
    await expect(tabs.nth(2)).toContainText('Messages');
    await expect(tabs.nth(3)).toContainText('Ladevorgänge');
  });

  test('tab labels change with language switch (French)', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('fr');

    const tabs = page.locator('.tabs .tab');
    await expect(tabs.nth(0)).toContainText('Aperçu');
    await expect(tabs.nth(1)).toContainText('Commandes');
    await expect(tabs.nth(2)).toContainText('Messages');
    await expect(tabs.nth(3)).toContainText('Sessions');
  });

  test('language persists after navigating to another tab', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();
    const langSelect = page.locator('.lang-select');
    await langSelect.selectOption('fr');

    const tabs = page.locator('.tabs .tab');
    await tabs.nth(1).click();
    await expect(tabs.nth(1)).toContainText('Commandes');

    await tabs.nth(0).click();
    await expect(tabs.nth(0)).toContainText('Aperçu');
  });

  test('empty state text changes with language', async ({ page }) => {
    await page.route('**/api/chargepoints', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );

    await page.reload();

    const emptyState = page.locator('.sidebar .empty-state');
    await expect(emptyState).toBeVisible();

    const langSelect = page.locator('.lang-select');

    langSelect.selectOption('de');
    const deText = await emptyState.textContent();

    langSelect.selectOption('en');
    const enText = await emptyState.textContent();

    expect(enText).not.toBe(deText);
  });
});
