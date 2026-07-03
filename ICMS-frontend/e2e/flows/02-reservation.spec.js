import { test, expect } from '@playwright/test';
import { loginViaLocalStorage } from '../helpers/auth';

test.describe('E2E 流程 2: 学生预约教室', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaLocalStorage(page, 'student');
  });

  test('查询空闲教室并提交预约', async ({ page }) => {
    await page.goto('/#/classroom/free');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('button:has-text("查询空闲教室")')).toBeVisible({ timeout: 5000 });

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dateStr = tomorrow.toISOString().slice(0, 10);
    const dateInput = page.locator('input[placeholder="选择日期"]');
    await dateInput.fill(dateStr);
    await dateInput.press('Enter');

    // 选择开始节次 (第3节)
    await page.locator('.el-select').nth(0).click();
    await page.waitForTimeout(500);
    await page.locator('.el-select-dropdown__item:visible').nth(2).click();
    await page.waitForTimeout(500);

    // 选择结束节次 (第4节)
    await page.locator('.el-select').nth(1).click();
    await page.waitForTimeout(500);
    await page.locator('.el-select-dropdown__item:visible').nth(3).click();
    await page.waitForTimeout(500);

    // 点击查询
    await page.click('button:has-text("查询空闲教室")');
    await page.waitForTimeout(800);

    // 点击第一个"预约"按钮
    const reserveBtn = page.locator('button:has-text("预约")').first();
    if (await reserveBtn.isVisible({ timeout: 3000 })) {
      await reserveBtn.click();
      await page.waitForTimeout(500);

      const purposeInput = page.locator('textarea[placeholder*="用途"]');
      if (await purposeInput.isVisible()) {
        await purposeInput.fill('E2E 自动化测试');
      }

      const phoneInput = page.locator('input[placeholder="请输入联系电话"]');
      if (await phoneInput.isVisible()) {
        await phoneInput.fill('13800138000');
      }

      await page.click('button:has-text("提交预约申请")');
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });
});
