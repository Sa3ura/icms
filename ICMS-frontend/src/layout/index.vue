<template>
  <el-container class="layout-container">
    <!-- 侧边栏 (极简无界设计) -->
    <el-aside
      v-if="!isMobile"
      id="desktop-navigation"
      :width="isCollapsed ? '80px' : '260px'"
      class="aside-menu"
    >
      <div
        class="logo-area"
        :class="{ 'is-collapsed': isCollapsed }"
      >
        <el-icon
          size="26"
          color="var(--text-main)"
        >
          <School />
        </el-icon>
        <span
          class="logo-title"
          :class="{ 'logo-title-collapsed': isCollapsed }"
        >ICMS</span>
      </div>
      
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          router
          class="sidebar-el-menu"
          :background-color="menuBg"
          :text-color="menuTextColor"
          :active-text-color="menuActiveColor"
        >
          <el-menu-item 
            v-for="item in menuRoutes" 
            :key="item.path" 
            :index="'/' + item.path"
          >
            <el-icon>
              <component :is="item.meta.icon" />
            </el-icon>
            <template #title>
              {{ item.meta.title }}
            </template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="main-container">
      <!-- 顶部 Header (轻薄悬浮感) -->
      <el-header class="header-area glass-header">
        <div class="header-left">
          <button
            type="button"
            class="collapse-btn hover-bg"
            :aria-label="isMobile ? '打开导航菜单' : isCollapsed ? '展开侧栏' : '收起侧栏'"
            :aria-expanded="isMobile ? mobileMenuOpen : !isCollapsed"
            :aria-controls="isMobile ? 'mobile-navigation' : 'desktop-navigation'"
            @click="toggleNavigation"
          >
            <el-icon size="20">
              <Menu v-if="isMobile" />
              <Fold v-else-if="!isCollapsed" />
              <Expand v-else />
            </el-icon>
          </button>

          <span
            v-if="isMobile"
            class="mobile-page-title"
          >{{ currentRouteTitle || 'ICMS' }}</span>
          
          <el-breadcrumb
            v-else
            separator="/"
          >
            <el-breadcrumb-item :to="{ path: '/' }">
              Dashboard
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRouteTitle">
              {{ currentRouteTitle }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 主题切换 -->
          <ThemeToggle />

          <!-- 消息通知 -->
          <el-dropdown
            trigger="click"
            placement="bottom-end"
          >
            <button
              type="button"
              class="action-btn hover-bg notice-badge"
              :aria-label="`通知中心，${unreadNoticesCount} 条未读`"
            >
              <el-badge
                :value="unreadNoticesCount"
                :max="9"
                :hidden="unreadNoticesCount === 0"
              >
                <el-icon
                  size="18"
                  class="bell-icon"
                >
                  <Bell />
                </el-icon>
              </el-badge>
            </button>
            <template #dropdown>
              <div class="notice-panel">
                <div class="notice-header">
                  <span class="notice-title-text">通知中心 ({{ unreadNoticesCount }})</span>
                  <div class="notice-header-actions">
                    <el-radio-group
                      v-model="noticeFilter"
                      size="small"
                      @change="() => {}"
                    >
                      <el-radio-button value="all">
                        全部
                      </el-radio-button>
                      <el-radio-button value="unread">
                        未读
                      </el-radio-button>
                    </el-radio-group>
                  </div>
                  <span
                    v-if="unreadNoticesCount > 0"
                    class="notice-clear"
                    @click="clearNotices"
                  >全部已读</span>
                </div>
                <el-scrollbar max-height="360px">
                  <div
                    v-if="notices.length > 0"
                    class="notice-list"
                  >
                    <div
                      v-for="item in filteredNotices"
                      :key="item.id"
                      class="notice-item"
                      :class="{ 'is-read': item.is_read }"
                      @click="handleNoticeClick(item)"
                    >
                      <div class="notice-item-header">
                        <span
                          class="dot"
                          :class="[item.notice_type, { 'dot-dim': item.is_read }]"
                        />
                        <span class="notice-item-title">{{ item.title }}</span>
                        <el-tag
                          v-if="!item.is_read"
                          size="small"
                          type="danger"
                          effect="plain"
                          class="unread-tag"
                        >
                          新
                        </el-tag>
                      </div>
                      <p class="notice-desc">
                        {{ item.content }}
                      </p>
                      <span class="notice-time">{{ item.created_at }}</span>
                    </div>
                  </div>
                  <div
                    v-else
                    class="empty-notice"
                  >
                    <el-icon
                      size="40"
                      class="empty-icon"
                    >
                      <BellFilled />
                    </el-icon>
                    <p>暂无新通知</p>
                  </div>
                </el-scrollbar>
                <div
                  v-if="notices.length > 0"
                  class="notice-footer"
                  @click="goToNotices"
                >
                  查看全部通知
                </div>
              </div>
            </template>
          </el-dropdown>

          <div class="divider-v" />

          <!-- 用户信息下拉 -->
          <el-dropdown
            trigger="click"
            placement="bottom-end"
            @command="handleCommand"
          >
            <button
              type="button"
              class="user-profile hover-bg"
              :aria-label="`账户菜单：${userStore.realName}`"
            >
              <el-avatar
                :size="32"
                class="user-avatar"
              >
                {{ userStore.realName.charAt(0) }}
              </el-avatar>
              <span
                v-if="!isCollapsed && !isMobile"
                class="user-info"
              >
                <span class="user-name">{{ userStore.realName }}</span>
                <span class="user-role">{{ getRoleName(userStore.role) }}</span>
              </span>
              <el-icon class="el-icon--right">
                <arrow-down />
              </el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown-menu">
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon> 账户安全
                </el-dropdown-item>
                <el-dropdown-item
                  divided
                  command="logout"
                  class="text-danger"
                >
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="content-main">
        <router-view v-slot="{ Component }">
          <transition
            name="fade-transform"
            mode="out-in"
          >
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
      <IcpFooter class="layout-footer" />
      <el-backtop
        target=".content-main"
        :right="isMobile ? 16 : 40"
        :bottom="40"
      />
    </el-container>

    <el-drawer
      v-if="isMobile"
      v-model="mobileMenuOpen"
      direction="ltr"
      size="min(82vw, 300px)"
      :with-header="false"
      class="mobile-nav-drawer"
      aria-label="主导航"
    >
      <nav
        id="mobile-navigation"
        class="mobile-navigation"
        aria-label="主导航"
      >
        <div class="mobile-navigation-header">
          <span class="mobile-brand"><el-icon :size="24"><School /></el-icon> ICMS</span>
          <button
            type="button"
            class="action-btn hover-bg"
            aria-label="关闭导航菜单"
            @click="mobileMenuOpen = false"
          >
            <el-icon :size="20">
              <Close />
            </el-icon>
          </button>
        </div>
        <el-scrollbar>
          <el-menu
            :default-active="activeMenu"
            router
            :background-color="menuBg"
            :text-color="menuTextColor"
            :active-text-color="menuActiveColor"
            @select="mobileMenuOpen = false"
          >
            <el-menu-item
              v-for="item in menuRoutes"
              :key="item.path"
              :index="'/' + item.path"
            >
              <el-icon><component :is="item.meta.icon" /></el-icon>
              <span>{{ item.meta.title }}</span>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>
      </nav>
    </el-drawer>
    
    <!-- 修改密码弹窗 (现代化) -->
    <el-dialog
      v-model="pwdDialogVisible"
      title="账户安全设置"
      width="420px"
      destroy-on-close
      class="modern-dialog"
    >
      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-position="top"
      >
        <el-form-item
          label="当前密码"
          prop="oldPassword"
        >
          <el-input
            v-model="pwdForm.oldPassword"
            type="password"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item
          label="新密码"
          prop="newPassword"
        >
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item
          label="确认新密码"
          prop="confirmPassword"
        >
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            show-password
            size="large"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="pwdDialogVisible = false">
            取消
          </el-button>
          <el-button
            type="primary"
            @click="submitPasswordChange"
          >
            保存更改
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 通知详情阅读弹窗 -->
    <el-dialog
      v-model="noticeDialogVisible"
      title="通知详情"
      width="500px"
      destroy-on-close
    >
      <div
        v-if="currentNotice"
        class="notice-detail-container"
      >
        <h3 class="detail-title">
          {{ currentNotice.title }}
        </h3>
        <div class="detail-meta">
          <el-tag
            size="small"
            :type="currentNotice.notice_type === 'warning' ? 'warning' : currentNotice.notice_type === 'system' ? 'danger' : 'primary'"
            class="detail-tag"
          >
            {{ currentNotice.notice_type === 'warning' ? '警告' : currentNotice.notice_type === 'system' ? '系统' : '普通' }}
          </el-tag>
          <span>发布人：{{ currentNotice.publisher_name || '系统管理员' }}</span>
          <span>时间：{{ currentNotice.created_at }}</span>
        </div>
        <el-divider border-style="dashed" />
        <div class="detail-content">
          {{ currentNotice.content }}
        </div>
      </div>
      <template #footer>
        <div
          class="dialog-footer"
          style="text-align: center;"
        >
          <el-button
            type="primary"
            style="width: 120px;"
            @click="noticeDialogVisible = false"
          >
            我知道了
          </el-button>
        </div>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '../store/user';
import { ElMessage } from 'element-plus';
import { apiGetNotices, apiChangePassword, apiMarkNoticeRead, apiMarkAllNoticesRead } from '../api';
import IcpFooter from '../components/IcpFooter.vue';
import ThemeToggle from '../components/ThemeToggle.vue';
import { useMediaQuery } from '../composables/useMediaQuery';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isCollapsed = ref(false);
const isMobile = useMediaQuery('(max-width: 1023px)');
const mobileMenuOpen = ref(false);
const toggleNavigation = () => {
  if (isMobile.value) mobileMenuOpen.value = !mobileMenuOpen.value;
  else isCollapsed.value = !isCollapsed.value;
};

watch([isMobile, () => route.fullPath], () => { mobileMenuOpen.value = false; });
const pwdDialogVisible = ref(false);
const noticeDialogVisible = ref(false);
const currentNotice = ref(null);
const pwdFormRef = ref(null);
const notices = ref([]);
const noticeFilter = ref('all');
let pollTimer = null;

const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) {
          callback(new Error('两次输入密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

// 获取可用路由菜单
const menuRoutes = computed(() => {
  const rootRoute = router.options.routes.find(r => r.path === '/');
  if (!rootRoute || !rootRoute.children) return [];
  return rootRoute.children.filter(item => {
    return !item.meta.roles || item.meta.roles.includes(userStore.role);
  });
});

const activeMenu = computed(() => route.path);
const currentRouteTitle = computed(() => route.meta.title || '');

// 主题样式绑定 (极简风格的背景)
const menuBg = computed(() => 'transparent');
const menuTextColor = computed(() => userStore.theme === 'dark' ? '#9ca3af' : '#4b5563');
const menuActiveColor = computed(() => userStore.theme === 'dark' ? '#ffffff' : '#111827');

const unreadNoticesCount = computed(() => notices.value.filter(n => !n.is_read).length);

const filteredNotices = computed(() => {
  if (noticeFilter.value === 'unread') {
    return notices.value.filter(n => !n.is_read);
  }
  return notices.value;
});

const getRoleName = (role) => {
  const mapping = {
    admin: 'Administrator',
    jw: 'Edu Admin',
    teacher: 'Teacher',
    student: 'Student',
    repairer: 'Repairer'
  };
  return mapping[role] || 'Guest';
};

const goToNotices = () => {
  router.push('/system/notices');
};

const handleCommand = async (command) => {
  if (command === 'logout') {
    await userStore.logout();
    ElMessage.success('已安全退出系统');
    window.location.hash = '#/login';
    window.location.reload();
  } else if (command === 'password') {
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' };
    pwdDialogVisible.value = true;
  }
};

const submitPasswordChange = () => {
  pwdFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        await apiChangePassword(
          userStore.userInfo.id,
          pwdForm.value.oldPassword,
          pwdForm.value.newPassword,
          userStore.userInfo.username
        );
        ElMessage.success('密码修改成功');
        pwdDialogVisible.value = false;
        pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' };
      } catch (err) {
        ElMessage.error(err.message || '密码修改失败');
      }
    }
  });
};

const loadNotices = async () => {
  try {
    const userId = userStore.userInfo?.id;
    const res = await apiGetNotices({ userId, pageSize: 10000 });
    if (res.code === 200) {
      notices.value = res.data.records
        .filter(notice => !notice.target_role || notice.target_role === userStore.role)
        .sort((a, b) => (a.is_read === b.is_read ? 0 : a.is_read ? 1 : -1));
    }
  } catch (error) {
    console.error('Failed to load notices', error);
  }
};

const handleNoticeClick = async (notice) => {
  currentNotice.value = notice;
  noticeDialogVisible.value = true;
  if (!notice.is_read) {
    try {
      await apiMarkNoticeRead(notice.id, userStore.userInfo.id);
      const idx = notices.value.findIndex(n => n.id === notice.id);
      if (idx !== -1) notices.value[idx].is_read = true;
    } catch (_) {
      // swallow
    }
  }
};

const clearNotices = async () => {
  try {
    await apiMarkAllNoticesRead(userStore.role, userStore.userInfo.id);
    notices.value.forEach(n => { n.is_read = true; });
    ElMessage.success('所有通知已标记为已读');
  } catch (_) {
    ElMessage.error('操作失败');
  }
};

onMounted(() => {
  loadNotices();
  pollTimer = setInterval(loadNotices, 30000);
});

onUnmounted(() => {
  clearInterval(pollTimer);
  pollTimer = null;
});
</script>

<style scoped>
.layout-container {
  height: 100vh;
  height: 100dvh;
  width: 100%;
  overflow: hidden;
  background-color: var(--bg-app);
}

/* 侧边栏 */
.aside-menu {
  background-color: var(--bg-sidebar);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  z-index: 10;
}

.logo-area {
  height: 56px;
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0 16px 0 44px;
  transition: padding 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.logo-area.is-collapsed {
  padding: 0;
  justify-content: center;
}

.logo-title {
  color: var(--text-main);
  font-weight: 800;
  font-size: 18px;
  letter-spacing: -0.5px;
  margin-left: 10px;
  white-space: nowrap;
  overflow: hidden;
  transition: opacity 0.15s cubic-bezier(0.4, 0, 0.2, 1), max-width 0.28s cubic-bezier(0.4, 0, 0.2, 1), margin-left 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: 200px;
}

.logo-title-collapsed {
  opacity: 0;
  max-width: 0;
  margin-left: 0;
  pointer-events: none;
}

.sidebar-el-menu {
  flex-grow: 1;
  padding: 0 12px;
  border-right: none;
}
.sidebar-el-menu.el-menu--collapse {
  padding: 0;
  width: 80px;
}

/* 顶部 Header */
.main-container {
  height: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.layout-footer {
  border-top: 1px solid var(--border-light);
  background-color: var(--bg-app);
}

.header-area {
  height: 72px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  border-bottom: 1px solid var(--border-light);
  background-color: var(--bg-header);
  z-index: 5;
}

.glass-header {
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left {
  min-width: 0;
}

.header-right {
  flex-shrink: 0;
}

.collapse-btn,
.action-btn,
.user-profile {
  border: 0;
  background: transparent;
  color: var(--text-regular);
  font: inherit;
}

.collapse-btn:focus-visible,
.action-btn:focus-visible,
.user-profile:focus-visible {
  outline: 2px solid var(--accent-color);
  outline-offset: 2px;
}

/* 交互按钮统一抽象 */
.hover-bg {
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s;
}
.hover-bg:hover {
  background-color: var(--border-light);
}

.action-btn {
  width: 40px;
  height: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--text-regular);
}

.collapse-btn {
  width: 40px;
  height: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--text-main);
  margin-left: -8px;
}

/* 面包屑重置 */
.el-breadcrumb {
  font-size: 14px;
}
:deep(.el-breadcrumb__inner) {
  font-weight: 500 !important;
  color: var(--text-secondary) !important;
}
:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-main) !important;
}

.divider-v {
  width: 1px;
  height: 24px;
  background-color: var(--border-medium);
  margin: 0 8px;
}

/* 用户信息 */
.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  margin-right: -12px;
}

.user-avatar {
  background-color: var(--primary-color);
  color: var(--bg-app);
  font-weight: 600;
  font-size: 14px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-main);
  line-height: 1.2;
}

.user-role {
  font-size: 11px;
  color: var(--text-secondary);
}

/* 通知下拉菜单重构 */
.notice-panel {
  width: 360px;
  max-width: calc(100vw - 24px);
  background-color: var(--bg-card);
  border-radius: 16px;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-float);
  overflow: hidden;
}
.notice-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  background-color: var(--bg-card);
}
.notice-header-actions {
  flex: 1;
  display: flex;
  justify-content: center;
}
.notice-title-text {
  font-weight: 700;
  font-size: 15px;
  color: var(--text-main);
}
.notice-clear {
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}
.notice-clear:hover {
  color: var(--primary-color);
}
.notice-item {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  transition: background-color 0.2s;
}
.notice-item:hover {
  background-color: var(--primary-light);
}
.notice-item.is-read {
  opacity: 0.6;
}
.notice-item:last-child {
  border-bottom: none;
}
.notice-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.notice-item-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-main);
  line-height: 1.4;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.unread-tag {
  font-size: 10px !important;
  padding: 0 4px !important;
  height: 18px !important;
  line-height: 18px !important;
  flex-shrink: 0;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--info-color);
  flex-shrink: 0;
}
.dot.dot-dim {
  background-color: var(--border-medium);
}
.dot.warning { background-color: var(--warning-color); }
.dot.info { background-color: var(--primary-color); }
.dot.system { background-color: var(--danger-color); }

.notice-desc {
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.5;
  margin: 0 0 8px 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.notice-time {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: 14px;
}
.notice-footer {
  padding: 14px;
  text-align: center;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  border-top: 1px solid var(--border-light);
  cursor: pointer;
  background-color: var(--bg-card);
  transition: all 0.2s;
}
.notice-footer:hover {
  color: var(--text-main);
  background-color: var(--primary-light);
}
.empty-notice {
  padding: 40px 20px;
  text-align: center;
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.empty-icon {
  opacity: 0.2;
}

/* 通知详情弹窗 */
.notice-detail-container {
  padding: 10px 0;
}
.detail-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 16px;
  line-height: 1.4;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
  flex-wrap: wrap;
}
.detail-content {
  font-size: 15px;
  color: var(--text-regular);
  line-height: 1.8;
  white-space: pre-wrap;
  padding: 10px 0 20px;
}

.text-danger {
  color: var(--danger-color) !important;
}

/* 路由转场动画: fade-transform */
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 内容区 */
.content-main {
  flex-grow: 1;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  background-color: transparent;
}

:global(.mobile-nav-drawer) {
  background-color: var(--bg-sidebar);
}

:global(.mobile-nav-drawer .el-drawer__body) {
  padding: 0;
  overflow: hidden;
}

.mobile-navigation {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom, 0px);
}

.mobile-navigation :deep(.el-scrollbar) {
  flex: 1;
  min-height: 0;
}

.mobile-navigation-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border-light);
}

.mobile-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 800;
  color: var(--text-main);
}

@media (max-width: 1023px) {
  .header-area {
    height: 60px;
    padding: 0 max(12px, env(safe-area-inset-right, 0px)) 0 max(12px, env(safe-area-inset-left, 0px));
    gap: 8px;
  }

  .header-left {
    flex: 1;
    gap: 4px;
    overflow: hidden;
  }

  .header-right {
    gap: 4px;
  }

  .collapse-btn,
  .action-btn,
  .user-profile {
    width: 44px;
    height: 44px;
    flex-shrink: 0;
    padding: 0;
    margin: 0;
  }

  .user-profile {
    justify-content: center;
  }

  .user-profile > .el-icon,
  .divider-v {
    display: none;
  }

  .mobile-page-title {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-main);
    font-size: 15px;
    font-weight: 600;
  }

  .content-main {
    padding: 16px max(16px, env(safe-area-inset-right, 0px)) 20px max(16px, env(safe-area-inset-left, 0px));
    overscroll-behavior-y: contain;
  }

  .layout-footer {
    padding-top: 8px;
    padding-bottom: max(8px, env(safe-area-inset-bottom, 0px));
    font-size: 11px;
  }

  .notice-header {
    flex-wrap: wrap;
    padding: 12px;
  }

  .notice-header-actions {
    flex: none;
    margin-left: auto;
  }
}
</style>
