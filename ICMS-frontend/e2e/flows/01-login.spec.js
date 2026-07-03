import { test, expect } from '@playwright/test';
import { loginAs } from '../helpers/auth';

test('admin 登录到仪表盘', async ({ page }) => {
  await loginAs(page, 'admin');
  await expect(page.locator('.metric-card').first()).toBeVisible();
});

test('登录失败显示错误', async ({ page }) => {
  await page.goto('/');
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 5000 });
  await page.fill('input[placeholder="请输入用户名"]', 'nobody');
  await page.fill('input[placeholder="请输入密码"]', 'wrong');
  await page.fill('input[placeholder="输入验证码"]', 'XXXX');
  await page.click('button:has-text("登 录")');
  await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 5000 });
});
