import { test, expect } from '@playwright/test';

test('真实后端登录 → 仪表盘', async ({ page }) => {
  await page.goto('/');
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15000 });
  await page.fill('input[placeholder="请输入用户名"]', 'admin');
  await page.fill('input[placeholder="请输入密码"]', '123456');
  await page.fill('input[placeholder="输入验证码"]', 'ABCD');
  await page.click('button:has-text("登 录")');
  await page.waitForSelector('.metric-card', { timeout: 15000 });
  await expect(page.locator('.user-name')).toHaveText('系统管理员');
});

test('真实后端登录失败', async ({ page }) => {
  await page.goto('/');
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15000 });
  await page.fill('input[placeholder="请输入用户名"]', 'admin');
  await page.fill('input[placeholder="请输入密码"]', 'wrong');
  await page.fill('input[placeholder="输入验证码"]', 'XXXX');
  await page.click('button:has-text("登 录")');
  await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 10000 });
});
