import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 5_000 },
  use: {
    baseURL: 'http://127.0.0.1:4201',
    headless: true,
    trace: 'retain-on-failure'
  },
  webServer: {
    command: 'npm run start',
    url: 'http://127.0.0.1:4201',
    reuseExistingServer: true,
    timeout: 120_000
  }
});
