import { defineConfig, devices } from '@playwright/test';

const port = 4300;

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: {
    timeout: 10_000
  },
  use: {
    baseURL: `http://127.0.0.1:${port}`,
    trace: 'on-first-retry'
  },
  projects: [
    {
      name: 'Chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ],
  webServer: {
    command: 'npm run start -- --host 0.0.0.0 --port ' + port,
    port,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000
  }
});
