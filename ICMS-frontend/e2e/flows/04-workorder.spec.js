import { test, expect } from '@playwright/test';
import { loginViaLocalStorage } from '../helpers/auth';

test.describe('E2E 流程 4: 工单管理', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaLocalStorage(page, 'admin');
  });

  test('新增工单并提交', async ({ page }) => {
    await page.goto('/#/work-order/list');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 5000 });

    await page.click('button:has-text("提交故障报修")');
    await page.waitForTimeout(500);

    // 选择教室
    await page.locator('.el-dialog .el-select').first().click();
    await page.waitForTimeout(500);
    await page.locator('.el-select-dropdown__item:visible').first().click();
    await page.waitForTimeout(500);

    // 填写故障标题
    await page.locator('input[placeholder*="简单描述"]').fill('E2E 测试工单');

    // 填写故障描述
    await page.locator('textarea[placeholder*="描述清晰"]').fill('自动化测试创建的故障工单');

    // 提交
    await page.locator('.el-dialog button:has-text("提交报修")').click();
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
  });
});
