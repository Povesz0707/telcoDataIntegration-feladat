import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: 'list',
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  webServer: process.env.BASE_URL
    ? undefined
    : {
        command: 'npm start -- --host 127.0.0.1 --port 4200',
        url: 'http://127.0.0.1:4200',
        reuseExistingServer: true,
        timeout: 120_000
      }
});
