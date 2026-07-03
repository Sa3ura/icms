import { test, expect } from '@playwright/test';
import { loginViaLocalStorage } from '../helpers/auth';

test.describe('E2E 流程 5: 用户管理', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaLocalStorage(page, 'admin');
  });

  test('新增用户并确认可见', async ({ page }) => {
    await page.goto('/#/system/user');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 5000 });

    await page.click('button:has-text("新增系统账号")');
    await page.waitForTimeout(500);

    const username = `e2e_${Date.now()}`;
    const inputs = page.locator('.el-dialog .el-input__inner');
    await inputs.nth(0).fill(username);
    await inputs.nth(1).fill('E2E 测试');

    // 选择角色（弹窗内的 el-select）
    await page.locator('.el-dialog .el-select').first().click();
    await page.waitForTimeout(500);
    await page.locator('.el-select-dropdown__item:visible:has-text("学生")').click();
    await page.waitForTimeout(300);

    await inputs.nth(2).fill('13800138000');
    await inputs.nth(3).fill('e2e@test.com');

    await page.locator('.el-dialog button:has-text("确定")').click();
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });

    await page.waitForTimeout(500);
    await expect(page.locator(`.el-table__body:has-text("${username}")`)).toBeVisible({ timeout: 5000 });
  });
});
