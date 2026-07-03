import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './flows',
  timeout: 30000,
  retries: 1,
  use: {
    baseURL: 'http://localhost:5173',
    screenshot: 'on',
    video: 'retain-on-failure',
    trace: 'on-first-retry'
  },
  webServer: {
    command: 'npm run dev:e2e',
    url: 'http://localhost:5173',
    reuseExistingServer: false,
    timeout: 60000
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } }
  ]
});
