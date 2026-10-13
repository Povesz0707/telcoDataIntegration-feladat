import { APIRequestContext, Page, expect, test } from '@playwright/test';

async function loginApi(request: APIRequestContext): Promise<{ Authorization: string }> {
  for (let attempt = 0; attempt < 30; attempt += 1) {
    try {
      const response = await request.post('/api/auth/login', {
        data: { username: 'telco_admin', password: 'telco_admin12345' }
      });
      if (response.ok()) {
        const body = await response.json() as { accessToken: string };
        return { Authorization: `Bearer ${body.accessToken}` };
      }
    } catch {
      // The isolated E2E backend can still be starting up.
    }

    await new Promise(resolve => setTimeout(resolve, 1_000));
  }

  throw new Error('The isolated E2E backend authentication did not become ready in time.');
}

async function waitForBackend(request: APIRequestContext, headers: { Authorization: string }): Promise<void> {
  for (let attempt = 0; attempt < 30; attempt += 1) {
    try {
      const response = await request.get('/api/processing-runs', { headers });
      if (response.ok()) {
        return;
      }
    } catch {
      // The isolated E2E backend can still be starting up.
    }

    await new Promise(resolve => setTimeout(resolve, 1_000));
  }

  throw new Error('The isolated E2E backend did not become ready in time.');
}

test.describe('normalizált termékek felület', () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async ({ request }) => {
    const headers = await loginApi(request);
    await waitForBackend(request, headers);

    const csv = [
      'sku,product_name,gross_price_huf,stock_qty,brand',
      'P-9001,Playwright Widget,12700,10,ACME',
      'P-9002,Playwright Cable,2540,2,WireCo',
      'P-9003,Incomplete Product,,3,ACME'
    ].join('\n');
    const json = JSON.stringify([
      {
        id: 'SKU9001',
        name: 'Playwright Widget',
        netPrice: 10000,
        currency: 'HUF',
        vatRate: 0.27,
        quantityAvailable: 9,
        manufacturer: 'ACME',
        ean: '599900000001',
        updatedAt: '2025-10-01T10:00:00Z'
      },
      {
        id: 'SKU9002',
        name: 'Playwright Cable',
        netPrice: 2000,
        currency: 'HUF',
        vatRate: 0.27,
        quantityAvailable: null,
        manufacturer: 'WireCo',
        ean: '599900000002',
        updatedAt: '2025-10-02T10:00:00Z'
      },
      {
        id: 'SKU9004',
        name: 'Invalid Product',
        netPrice: null,
        currency: 'HUF',
        vatRate: 0.27,
        quantityAvailable: 1,
        manufacturer: 'ACME',
        ean: null,
        updatedAt: '2025-10-04T10:00:00Z'
      }
    ]);

    const sourceUpload = await request.post('/api/uploads/sources', {
      headers,
      multipart: {
        csvFile: {
          name: 'playwright.csv',
          mimeType: 'text/csv',
          buffer: Buffer.from(csv)
        },
        jsonFile: {
          name: 'playwright.json',
          mimeType: 'application/json',
          buffer: Buffer.from(json)
        }
      }
    });
    const sourceUploadBody = await sourceUpload.text();
    expect(sourceUpload.ok(), `Source upload failed with ${sourceUpload.status()}: ${sourceUploadBody}`).toBeTruthy();

    const uploadedSources = JSON.parse(sourceUploadBody);
    const processing = await request.post('/api/processing-runs', {
      headers,
      data: {
        csvBatchId: uploadedSources.csv.batchId,
        jsonBatchId: uploadedSources.json.batchId,
        primarySource: 'JSON',
        fallbackMissingValues: true
      }
    });
    expect(processing.ok()).toBeTruthy();
  });

  async function login(page: Page): Promise<void> {
    await page.goto('/');
    const loginButton = page.getByRole('button', { name: 'Bejelentkezés' });
    if (await loginButton.isVisible()) {
      await page.getByLabel('Felhasználónév').fill('telco_admin');
      await page.getByLabel('Jelszó').fill('telco_admin12345');
      await loginButton.click();
    }
    await expect(page.getByRole('heading', { name: 'Termékadatok normalizálása' })).toBeVisible();
  }

  test('betölti a legutóbbi futást és megjeleníti a termékeket', async ({ page }) => {
    await login(page);

    await expect(page.getByRole('heading', { name: 'Termékadatok normalizálása' })).toBeVisible();
    await expect(page.getByText('Normalizált termékek', { exact: true })).toBeVisible();
    await expect(page.getByText(/Valid: \d+/)).toBeVisible();
    await expect(page.locator('app-product-table tbody tr')).toHaveCount(4);
    await expect(page.locator('app-product-table tbody tr').filter({ hasText: 'SKU9001' })).toBeVisible();
  });

  test('szűrés után az üres eredményállapot is megjelenik', async ({ page }) => {
    await login(page);
    const skuFilter = page.locator('app-product-table').getByPlaceholder('Szűrés...').first();

    await skuFilter.fill('SKU9001');
    await page.waitForTimeout(450);
    await expect(page.locator('app-product-table tbody tr')).toHaveCount(1);

    await skuFilter.fill('nincs-ilyen-termek');
    await page.waitForTimeout(450);
    await expect(page.getByText('Nincs megjeleníthető rekord.', { exact: true })).toBeVisible();
  });

  test('issue dialogban külön jelennek meg a warningok és az errorok', async ({ page }) => {
    await login(page);
    const row = page.locator('app-product-table tbody tr').filter({ hasText: 'SKU9001' });
    await row.getByRole('button', { name: 'Jelzések megnyitása' }).click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Issue-k');
    await expect(dialog).toContainText('Figyelmeztetések');
    await expect(dialog).toContainText('Mező');
  });

  test('a feldolgozások accordion alapból zárt, majd megnyitható', async ({ page }) => {
    await login(page);
    const historyButton = page.getByRole('button', { name: /Feldolgozások/ });
    await expect(historyButton).toHaveAttribute('aria-expanded', 'false');
    await expect(page.locator('app-processing-runs-table')).toBeHidden();

    await historyButton.click();
    await expect(page.locator('app-processing-runs-table table')).toBeVisible();
    await expect(page.locator('app-processing-runs-table .p-tag').filter({ hasText: 'Aktív' })).toBeVisible();
  });

  test('feltöltésnél a hibás fájltípus elutasításra kerül, valid fájloknál engedélyezett a feldolgozás', async ({ page }) => {
    await login(page);
    await page.getByRole('button', { name: 'Adatforrások feltöltése' }).click();
    const dialog = page.getByRole('dialog', { name: 'Adatforrások feltöltése' });
    const processButton = dialog.getByRole('button', { name: 'Feldolgozás' });

    await expect(processButton).toBeDisabled();
    await dialog.locator('#csvFileInput').setInputFiles({
      name: 'not-a-csv.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('invalid')
    });
    await expect(dialog.getByText('Nem megfelelő fájltípus.', { exact: true })).toBeVisible();

    await dialog.locator('#csvFileInput').setInputFiles({
      name: 'valid.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from('sku,product_name\nP-1,Product')
    });
    await dialog.locator('#jsonFileInput').setInputFiles({
      name: 'valid.json',
      mimeType: 'application/json',
      buffer: Buffer.from('[]')
    });
    await expect(processButton).toBeEnabled();
  });

  test('valid fájlokkal a felületről is elindítható a feldolgozás', async ({ page }) => {
    await login(page);
    await page.getByRole('button', { name: 'Adatforrások feltöltése' }).click();
    const dialog = page.getByRole('dialog', { name: 'Adatforrások feltöltése' });

    await dialog.locator('#csvFileInput').setInputFiles({
      name: 'ui-process.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from('sku,product_name,gross_price_huf,stock_qty,brand\nP-9010,UI Product,12700,4,ACME')
    });
    await dialog.locator('#jsonFileInput').setInputFiles({
      name: 'ui-process.json',
      mimeType: 'application/json',
      buffer: Buffer.from(JSON.stringify([{
        id: 'SKU9010',
        name: 'UI Product',
        netPrice: 10000,
        currency: 'HUF',
        vatRate: 0.27,
        quantityAvailable: 4,
        manufacturer: 'ACME',
        ean: '599900000010',
        updatedAt: '2025-10-10T10:00:00Z'
      }]))
    });

    const processButton = dialog.getByRole('button', { name: 'Feldolgozás' });
    await expect(processButton).toBeEnabled();
    await processButton.click();
    await expect(dialog).toBeHidden({ timeout: 15_000 });
    await expect(page.locator('app-product-table tbody tr').filter({ hasText: 'SKU9010' })).toBeVisible({
      timeout: 15_000
    });
  });

  test('API hiba esetén a felület hibaüzenetet mutat', async ({ page }) => {
    await login(page);
    await page.route('**/api/processing-runs/latest', route => route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'test error' })
    }));
    await page.route('**/api/processing-runs', route => route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'test error' })
    }));

    await page.goto('/');
    await expect(page.getByText('A feldolgozás közben szerverhiba történt.', { exact: true })).toBeVisible();
  });
});
