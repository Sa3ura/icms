<template>
  <div class="login-wrapper">
    <!-- 全屏氛围背景：渐变光斑 + 网格 -->
    <div class="bg-layer">
      <div class="bg-blob blob-1" />
      <div class="bg-blob blob-2" />
      <div class="bg-blob blob-3" />
      <div class="bg-grid" />
    </div>

    <!-- 毛玻璃登录浮层 -->
    <div class="glass-card">
      <div class="card-header">
        <div class="logo-area">
          <div class="logo-badge">
            <el-icon size="26">
              <School />
            </el-icon>
          </div>
          <span class="logo-text">ICMS</span>
        </div>
        <h2>欢迎回来</h2>
        <p>智慧教室管理系统 · 登录以继续</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        class="glass-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item
          label="用户名 / 手机号"
          prop="username"
        >
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item
          label="密码"
          prop="password"
        >
          <el-input
            v-model="loginForm.password"
            placeholder="请输入密码"
            type="password"
            size="large"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item
          label="安全验证"
          prop="captcha"
          class="captcha-item"
        >
          <div class="captcha-flex">
            <el-input
              v-model="loginForm.captcha"
              placeholder="输入验证码"
              maxlength="4"
              size="large"
            >
              <template #prefix>
                <el-icon><Key /></el-icon>
              </template>
            </el-input>
            <div
              ref="captchaRef"
              class="captcha-svg"
              title="点击刷新"
              @click="refreshCaptcha"
            />
          </div>
        </el-form-item>

        <div class="form-actions">
          <el-checkbox v-model="loginForm.rememberMe">
            保持登录
          </el-checkbox>
          <el-link
            underline="never"
            class="forgot-link"
            @click="forgotPassword"
          >
            忘记密码？
          </el-link>
        </div>

        <el-button
          type="primary"
          class="submit-btn"
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </el-button>
      </el-form>

      <!-- 快速体验通道 -->
      <div class="quick-access">
        <div class="divider">
          <span>一键体验</span>
        </div>
        <div class="role-list">
          <el-tooltip
            content="系统管理员 (admin)"
            placement="top"
          >
            <div
              class="role-avatar"
              @click="quickFill('admin')"
            >
              A
            </div>
          </el-tooltip>
          <el-tooltip
            content="教务老师 (jw_admin)"
            placement="top"
          >
            <div
              class="role-avatar"
              @click="quickFill('jw_admin')"
            >
              J
            </div>
          </el-tooltip>
          <el-tooltip
            content="任课教师 (teacher_wang)"
            placement="top"
          >
            <div
              class="role-avatar"
              @click="quickFill('teacher_wang')"
            >
              T
            </div>
          </el-tooltip>
          <el-tooltip
            content="维修人员 (repairer_zhang)"
            placement="top"
          >
            <div
              class="role-avatar"
              @click="quickFill('repairer_zhang')"
            >
              R
            </div>
          </el-tooltip>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../store/user';
import { apiGetCaptcha } from '../api';
import { ElMessage } from 'element-plus';

const router = useRouter();
const userStore = useUserStore();

const loginFormRef = ref(null);
const captchaRef = ref(null);
const loading = ref(false);
const captchaId = ref('');
const captchaSvg = ref('');

const loginForm = ref({
  username: '',
  password: '',
  captcha: '',
  rememberMe: false
});

const loginRules = {
  username: [{ required: true, message: '此项必填', trigger: 'blur' }],
  password: [{ required: true, message: '此项必填', trigger: 'blur' }],
  captcha: [{ required: true, message: '此项必填', trigger: 'blur' }]
};

const refreshCaptcha = async () => {
  try {
    const data = await apiGetCaptcha();
    if (data) {
      captchaId.value = data.captchaId;
      captchaSvg.value = data.svg;
    }
  } catch {
    ElMessage.error('验证码获取失败');
  }
};

watch(captchaSvg, async (svg) => {
  await nextTick();
  if (captchaRef.value) {
    captchaRef.value.innerHTML = svg || '';
  }
});

const quickFill = async (username) => {
  loginForm.value.username = username;
  loginForm.value.password = '123456';
  await refreshCaptcha();
  loginForm.value.captcha = '';
  handleLogin();
};

const handleLogin = () => {
  loginFormRef.value?.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const res = await userStore.login(
          loginForm.value.username,
          loginForm.value.password,
          captchaId.value,
          loginForm.value.captcha
        );
        if (res.code === 200) {
          ElMessage.success({ message: '欢迎回来', type: 'success' });
          router.push('/');
        }
      } catch (err) {
        ElMessage.error(err.message || '登录失败');
        refreshCaptcha();
        loginForm.value.captcha = '';
      } finally {
        loading.value = false;
      }
    }
  });
};

const forgotPassword = () => {
  ElMessage.info('请联系系统管理员重置密码');
};

onMounted(() => {
  refreshCaptcha();
});
</script>

<style scoped>
/* ============ 全屏氛围背景 ============ */
.login-wrapper {
  position: relative;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background-color: var(--bg-app);
}

.bg-layer {
  position: absolute;
  inset: 0;
  z-index: 0;
}

/* 网格纹理，弱对比 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--border-light) 1px, transparent 1px),
    linear-gradient(90deg, var(--border-light) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, #000 40%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, #000 40%, transparent 100%);
  opacity: 0.6;
}

/* 渐变光斑 */
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
}
html.dark .bg-blob { opacity: 0.4; }

.blob-1 {
  width: 520px;
  height: 520px;
  background: linear-gradient(135deg, #4f46e5, #818cf8);
  top: -120px;
  left: -100px;
  animation: blob-float 16s infinite ease-in-out;
}
.blob-2 {
  width: 460px;
  height: 460px;
  background: linear-gradient(135deg, #10b981, #34d399);
  bottom: -140px;
  right: -120px;
  animation: blob-float-reverse 19s infinite ease-in-out -4s;
}
.blob-3 {
  width: 360px;
  height: 360px;
  background: linear-gradient(135deg, #6366f1, #a855f7);
  top: 40%;
  left: 55%;
  animation: blob-float 22s infinite ease-in-out -8s;
}

@keyframes blob-float {
  0%   { transform: translate(0, 0) scale(1); }
  33%  { transform: translate(50px, -60px) scale(1.1); }
  66%  { transform: translate(-30px, 40px) scale(0.92); }
  100% { transform: translate(0, 0) scale(1); }
}
@keyframes blob-float-reverse {
  0%   { transform: translate(0, 0) scale(1); }
  33%  { transform: translate(-50px, 60px) scale(1.1); }
  66%  { transform: translate(30px, -40px) scale(0.92); }
  100% { transform: translate(0, 0) scale(1); }
}

/* ============ 毛玻璃登录卡片 ============ */
.glass-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 44px 40px 36px;
  border-radius: 24px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-float);
  animation: card-in 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes card-in {
  0%   { opacity: 0; transform: translateY(24px) scale(0.98); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}

.card-header {
  margin-bottom: 28px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.logo-badge {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--primary-color);
  color: var(--bg-app);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.logo-text {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 1px;
  color: var(--text-main);
}

.card-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 6px;
  letter-spacing: -0.5px;
}

.card-header p {
  font-size: 14px;
  color: var(--text-secondary);
}

/* ============ 表单 ============ */
.glass-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text-regular);
  padding-bottom: 6px;
  font-size: 13px;
}

.glass-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--border-medium) inset !important;
  background-color: rgba(255, 255, 255, 0.4) !important;
  border-radius: 10px !important;
  height: 48px;
  transition: all 0.2s ease;
}
html.dark .glass-form :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.04) !important;
}
.glass-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--primary-color) inset !important;
  background-color: var(--bg-card) !important;
}
.glass-form :deep(.el-input__inner) {
  font-size: 15px;
}
.glass-form :deep(.el-input__prefix) {
  color: var(--text-secondary);
}

.captcha-flex {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-svg {
  width: 120px;
  height: 48px;
  border-radius: 10px;
  border: 1px solid var(--border-medium);
  background-color: var(--bg-card);
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color 0.2s ease;
}
.captcha-svg:hover {
  border-color: var(--primary-color);
}
.captcha-svg :deep(svg) {
  width: 100%;
  height: 100%;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.forgot-link {
  font-size: 13px;
  color: var(--text-secondary);
}
.forgot-link:hover {
  color: var(--text-main);
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  background-color: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
  color: var(--bg-app) !important;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 28px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}
.submit-btn:hover {
  background-color: var(--primary-hover) !important;
  transform: translateY(-1px);
}

/* ============ 一键体验 ============ */
.quick-access .divider {
  display: flex;
  align-items: center;
  text-align: center;
  margin-bottom: 18px;
}
.quick-access .divider::before,
.quick-access .divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid var(--border-medium);
}
.quick-access .divider span {
  padding: 0 12px;
  color: var(--text-secondary);
  font-size: 12px;
}

.role-list {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.role-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background-color: var(--primary-light);
  color: var(--text-regular);
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: 700;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}
.role-avatar:hover {
  background-color: var(--bg-card);
  border-color: var(--primary-color);
  color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

/* ============ 响应式 ============ */
@media (max-width: 480px) {
  .glass-card {
    padding: 32px 24px 28px;
  }
  .card-header h2 {
    font-size: 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .bg-blob,
  .glass-card {
    animation: none;
  }
}




</style>
