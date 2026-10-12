import { defineConfig } from '@playwright/test';

const angularCli = process.platform === 'win32' ? '.\\node_modules\\.bin\\ng.cmd' : './node_modules/.bin/ng';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: 'list',
  use: {
    baseURL: process.env.BASE_URL ?? 'http://127.0.0.1:4201',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  webServer: process.env.BASE_URL
    ? undefined
    : {
        command: `${angularCli} serve --proxy-config proxy.e2e.conf.json --host 127.0.0.1 --port 4201`,
        url: 'http://127.0.0.1:4201',
        reuseExistingServer: false,
        timeout: 120_000
      }
});
