<template>
  <div class="login-wrapper">
    <div class="login-split-container">
      <!-- 左侧：极简抽象展示区 -->
      <div class="login-showcase">
        <div class="showcase-content">
          <div class="logo-area">
            <el-icon size="32">
              <School />
            </el-icon>
            <span class="logo-text">ICMS</span>
          </div>
          <div class="slogan-area">
            <h1>空间管理<br>化繁为简。</h1>
            <p>基于 IoT 与时空匹配算法，提供极致流畅的智慧校园全场景预约、排课与设备监控服务。</p>
          </div>
          <!-- 极简抽象图形装饰 -->
          <div class="abstract-shape shape-1" />
          <div class="abstract-shape shape-2" />
        </div>
      </div>

      <!-- 右侧：极净表单区 -->
      <div class="login-form-wrapper">
        <div class="login-form-inner">
          <div class="form-header">
            <h2>欢迎回来</h2>
            <p>登录您的账户以继续</p>
          </div>

          <el-form 
            ref="loginFormRef" 
            :model="loginForm" 
            :rules="loginRules" 
            label-position="top"
            class="minimal-form"
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
              />
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
              />
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
                />
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
/* 容器全屏极简背景 */
.login-wrapper {
  height: 100vh;
  width: 100vw;
  background-color: var(--bg-app);
  /* 增强点阵对比度，使日间模式清晰可见 */
  background-image: radial-gradient(var(--text-secondary) 1px, transparent 1px);
  background-size: 24px 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
}

/* 左右分栏容器，极柔和阴影，超大圆角 */
.login-split-container {
  width: 100%;
  max-width: 1200px;
  height: 720px;
  background-color: var(--bg-card);
  border-radius: 24px;
  box-shadow: 0 20px 40px -10px rgba(0,0,0,0.05), 0 10px 20px -5px rgba(0,0,0,0.02);
  display: flex;
  overflow: hidden;
  border: 1px solid var(--border-light);
}

/* 左侧：极简展示区 */
.login-showcase {
  flex: 1;
  background-color: #111827; /* 保持左侧固定深色作为强烈对比 */
  color: #ffffff;
  position: relative;
  overflow: hidden;
}

.showcase-content {
  padding: 80px;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 2;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 100px;
}

.logo-text {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 1px;
}

/* 动画定义 */
@keyframes blob-float {
  0% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(40px, -60px) scale(1.1); }
  66% { transform: translate(-30px, 30px) scale(0.9); }
  100% { transform: translate(0, 0) scale(1); }
}

@keyframes blob-float-reverse {
  0% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-40px, 60px) scale(1.1); }
  66% { transform: translate(30px, -30px) scale(0.9); }
  100% { transform: translate(0, 0) scale(1); }
}

@keyframes text-slide-up {
  0% { opacity: 0; transform: translateY(40px); }
  100% { opacity: 1; transform: translateY(0); }
}

.slogan-area h1 {
  font-size: 56px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 24px;
  letter-spacing: -1.5px;
  opacity: 0;
  animation: text-slide-up 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.2s forwards;
}

.slogan-area p {
  font-size: 18px;
  color: #9ca3af;
  line-height: 1.6;
  max-width: 80%;
  opacity: 0;
  animation: text-slide-up 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.4s forwards;
}

/* 抽象图形装饰 (莫兰迪色柔和渐变) */
.abstract-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  z-index: 1;
  opacity: 0.5;
}

.shape-1 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #4f46e5, #818cf8);
  bottom: -150px;
  right: -150px;
  animation: blob-float 15s infinite ease-in-out;
}

.shape-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #10b981, #34d399);
  top: 10%;
  left: -200px;
  animation: blob-float-reverse 18s infinite ease-in-out -5s;
}

/* 右侧：超白净表单区 */
.login-form-wrapper {
  width: 540px;
  background-color: var(--bg-card);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
}

.login-form-inner {
  width: 380px;
}

.form-header {
  margin-bottom: 40px;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.form-header p {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 覆盖局部 Form 样式 */
.minimal-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text-regular);
  padding-bottom: 6px;
  font-size: 13px;
}

.minimal-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--border-medium) inset !important;
  background-color: var(--bg-card) !important;
  border-radius: 8px !important;
  height: 48px;
}

.minimal-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--primary-color) inset !important;
}

.minimal-form :deep(.el-input__inner) {
  font-size: 15px;
}

.captcha-flex {
  display: flex;
  gap: 16px;
  width: 100%;
}

.captcha-svg {
  width: 120px;
  height: 48px;
  border-radius: 8px;
  border: 1px solid var(--border-medium);
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}
.captcha-svg :deep(svg) {
  width: 100%;
  height: 100%;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
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
  border-radius: 8px;
  background-color: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
  color: var(--bg-app) !important;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 32px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.submit-btn:hover {
  background-color: var(--primary-hover) !important;
  transform: translateY(-1px);
}

/* 一键体验 */
.quick-access .divider {
  display: flex;
  align-items: center;
  text-align: center;
  margin-bottom: 20px;
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
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--primary-light);
  color: var(--text-regular);
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: 700;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.role-avatar:hover {
  background-color: var(--bg-card);
  border-color: var(--border-medium);
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  transform: translateY(-2px);
  color: var(--primary-color);
}

/* 响应式调整 */
@media (max-width: 900px) {
  .login-split-container {
    flex-direction: column;
    height: auto;
  }
  .login-showcase {
    display: none;
  }
  .login-form-wrapper {
    width: 100%;
    padding: 60px 0;
  }
}
</style>
