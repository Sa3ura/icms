import { test, expect } from '@playwright/test';
import { loginAs } from '../helpers/auth';

test.use({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });

test('手机首页使用完整宽度，导航以抽屉打开并在选择后关闭', async ({ page }) => {
  await loginAs(page, 'admin');
  await expect(page.locator('.aside-menu')).toHaveCount(0);
  const cards = page.locator('.metric-card');
  await expect(cards).toHaveCount(4);
  const boxes = await cards.evaluateAll(elements => elements.map(element => {
    const { x, y, width } = element.getBoundingClientRect();
    return { x, y, width };
  }));
  expect(boxes[0].y).toBeCloseTo(boxes[1].y, 0);
  expect(boxes[1].x).toBeGreaterThan(boxes[0].x + boxes[0].width);
  expect(boxes[2].y).toBeGreaterThan(boxes[0].y);
  expect(await page.locator('.content-main').evaluate(element => element.scrollWidth - element.clientWidth)).toBeLessThanOrEqual(1);

  await page.getByRole('button', { name: '打开导航菜单' }).click();
  const navigation = page.getByRole('navigation', { name: '主导航' });
  await expect(navigation).toBeVisible();
  await navigation.getByRole('menuitem', { name: '教室资源管理' }).click();
  await expect(page).toHaveURL(/#\/classroom\/list$/);
  await expect(navigation).not.toBeVisible();
  await expect(page.getByRole('button', { name: '新增教室' })).toBeVisible();

  await page.getByRole('button', { name: '打开导航菜单' }).click();
  await expect(navigation).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(navigation).not.toBeVisible();
  await expect(page.getByRole('button', { name: '打开导航菜单' })).toBeFocused();
});

test('320px 屏幕可以完整使用日期范围和预约弹窗', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 740 });
  await loginAs(page, 'admin');
  await page.goto('/#/reservation/my');
  await page.locator('.el-date-editor--daterange .el-range-input').first().click();
  const calendar = page.locator('.el-date-range-picker:visible');
  await expect(calendar).toBeVisible();
  const calendarBox = await calendar.boundingBox();
  expect(calendarBox.x).toBeGreaterThanOrEqual(0);
  expect(calendarBox.x + calendarBox.width).toBeLessThanOrEqual(320);
  const firstMonth = await calendar.locator('.is-left').boundingBox();
  const secondMonth = await calendar.locator('.is-right').boundingBox();
  expect(secondMonth.y).toBeGreaterThanOrEqual(firstMonth.y + firstMonth.height);
  await page.keyboard.press('Escape');

  await page.getByRole('button', { name: '发起预约申请' }).click();
  const dialog = page.getByRole('dialog', { name: '新建教室预约申请' });
  await expect(dialog).toBeVisible();
  const dialogBox = await dialog.boundingBox();
  expect(dialogBox.x).toBeGreaterThanOrEqual(0);
  expect(dialogBox.x + dialogBox.width).toBeLessThanOrEqual(320);
  expect(await dialog.evaluate(element => element.scrollWidth - element.clientWidth)).toBeLessThanOrEqual(1);
  const selects = dialog.locator('.section-picker-row .el-select');
  await expect(selects).toHaveCount(2);
  for (const select of await selects.all()) {
    const box = await select.boundingBox();
    expect(box.x + box.width).toBeLessThanOrEqual(dialogBox.x + dialogBox.width);
  }
});

test('快速离开图表页面不会继续更新已销毁的图表', async ({ page }) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await loginAs(page, 'admin');
  for (const route of ['dashboard', 'environment/index', 'dashboard']) {
    await page.goto('/#/' + route);
    await page.locator('.chart-box').first().waitFor();
    await page.goto('/#/classroom/list');
    await expect(page.getByRole('button', { name: '新增教室' })).toBeVisible();
  }
  await page.goto('/#/dashboard');
  await expect(page.locator('.chart-box svg').first()).toBeVisible();
  expect(errors).toEqual([]);
});
