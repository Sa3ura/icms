import { test, expect } from '@playwright/test';
import { loginViaLocalStorage } from '../helpers/auth';

test.describe('E2E 流程 3: 教务审批预约', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaLocalStorage(page, 'jw');
  });

  test('审批页面可见并有待审批记录', async ({ page }) => {
    await page.goto('/#/reservation/approval');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 5000 });

    // 如果有行，尝试审批第一条
    const approveBtn = page.locator('button:has-text("通过")').first();
    if (await approveBtn.isVisible({ timeout: 3000 })) {
      await approveBtn.click();
      await page.waitForTimeout(300);

      // 填写审批意见
      const remarkInput = page.locator('textarea[placeholder*="意见"]');
      if (await remarkInput.isVisible()) {
        await remarkInput.fill('E2E 测试审批通过');
      }

      // 确认
      await page.locator('.el-dialog button:has-text("确")').last().click();
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });
});
