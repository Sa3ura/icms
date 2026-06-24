import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import { useUserStore } from './store/user';

// 引入 Element Plus
import ElementPlus from 'element-plus';
import { ElMessage } from 'element-plus';
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css'; // 暗黑模式支持
import * as ElementPlusIconsVue from '@element-plus/icons-vue';

// 导入全局样式
import './style.css';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(ElementPlus);

app.config.errorHandler = (err, instance, info) => {
  console.error('[ICMS Error]', err, info);
  ElMessage.error({
    message: '系统发生异常，请刷新页面后重试',
    duration: 5000
  });
};

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

// 初始化主题与用户状态
const userStore = useUserStore();
userStore.initTheme();

let refreshAttempted = false;

router.beforeEach(async (to, _from) => {
  const userStore = useUserStore();

  if (!refreshAttempted && !userStore.isLoggedIn) {
    refreshAttempted = true;
    const success = await userStore.tryRefresh();
    if (success && to.path === '/login') {
      return '/';
    }
  }

  const isLoggedIn = userStore.isLoggedIn;
  const userRole = userStore.role;

  if (to.matched.some(record => record.meta.requiresAuth !== false) && !isLoggedIn) {
    return '/login';
  }
  if (to.path === '/login' && isLoggedIn) {
    return '/';
  }
  if (to.meta.roles && !to.meta.roles.includes(userRole)) {
    return '/dashboard';
  }
  return true;
});

app.mount('#app');
