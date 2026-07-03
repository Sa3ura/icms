import { expect } from '@playwright/test';

export async function loginAs(page, username, password = '123456') {
  await page.goto('/');
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 10000 });
  await page.fill('input[placeholder="请输入用户名"]', username);
  await page.fill('input[placeholder="请输入密码"]', password);
  await page.fill('input[placeholder="输入验证码"]', 'ABCD');
  await page.click('button:has-text("登 录")');
  await page.waitForSelector('.metric-card', { timeout: 10000 });
}

export async function loginViaLocalStorage(page, role) {
  const accounts = {
    admin: { id: 1, username: 'admin', real_name: '管理员', user_type: 'admin' },
    jw: { id: 2, username: 'jw_admin', real_name: '赵教务', user_type: 'jw' },
    teacher: { id: 3, username: 'teacher_wang', real_name: '王老师', user_type: 'teacher' },
    student: { id: 4, username: 'student_li', real_name: '李同学', user_type: 'student' },
    repairer: { id: 5, username: 'repairer_zhang', real_name: '张师傅', user_type: 'repairer' }
  };

  const user = accounts[role] || accounts.admin;
  const token = `mock_jwt_token_${user.username}_${Date.now()}`;

  await page.addInitScript(({ info, tok }) => {
    localStorage.setItem('icms_token', tok);
    localStorage.setItem('icms_user_info', JSON.stringify(info));
    localStorage.setItem('icms_theme', 'light');
  }, { info: user, tok: token });

  await page.goto('/');
  await page.waitForSelector('.metric-card', { timeout: 10000 });

  const name = await page.locator('.user-name').textContent();
  expect(name).toBe(user.real_name);
}
