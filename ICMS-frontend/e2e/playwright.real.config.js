import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './flows',
  testMatch: '06-real-login.spec.js',
  timeout: 30000,
  retries: 0,
  use: {
    baseURL: 'http://localhost:5173',
    screenshot: 'on',
    video: 'retain-on-failure'
  },
  webServer: {
    command: 'npm run dev:e2e-real',
    url: 'http://localhost:5173',
    reuseExistingServer: false,
    timeout: 30000
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } }
  ]
});
