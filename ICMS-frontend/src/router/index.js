import { createRouter, createWebHashHistory } from 'vue-router';
import { useUserStore } from '../store/user';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { title: '首页仪表盘', icon: 'Odometer', roles: ['admin', 'jw', 'teacher', 'student', 'repairer'] }
      },
      {
        path: 'classroom/list',
        name: 'ClassroomList',
        component: () => import('../views/classroom/ClassroomList.vue'),
        meta: { title: '教室资源管理', icon: 'School', roles: ['admin', 'jw', 'teacher'] }
      },
      {
        path: 'classroom/free',
        name: 'FreeClassroom',
        component: () => import('../views/classroom/FreeClassroom.vue'),
        meta: { title: '空闲教室查询', icon: 'Search', roles: ['admin', 'jw', 'teacher', 'student'] }
      },
      {
        path: 'reservation/my',
        name: 'MyReservation',
        component: () => import('../views/reservation/MyReservation.vue'),
        meta: { title: '我的预约申请', icon: 'Calendar', roles: ['admin', 'jw', 'teacher', 'student'] }
      },
      {
        path: 'reservation/approval',
        name: 'Approval',
        component: () => import('../views/reservation/Approval.vue'),
        meta: { title: '教室预约审批', icon: 'Stamp', roles: ['admin', 'jw'] }
      },
      {
        path: 'device/list',
        name: 'DeviceList',
        component: () => import('../views/device/DeviceList.vue'),
        meta: { title: '设备资产管理', icon: 'Cpu', roles: ['admin', 'jw', 'teacher'] }
      },
      {
        path: 'work-order/list',
        name: 'WorkOrderList',
        component: () => import('../views/workorder/WorkOrderList.vue'),
        meta: { title: '报修工单管理', icon: 'Tools', roles: ['admin', 'jw', 'teacher', 'student', 'repairer'] }
      },
      {
        path: 'environment/index',
        name: 'Environment',
        component: () => import('../views/environment/index.vue'),
        meta: { title: '环境实时监测', icon: 'Compass', roles: ['admin', 'jw', 'teacher'] }
      },
      {
        path: 'system/user',
        name: 'User',
        component: () => import('../views/system/User.vue'),
        meta: { title: '用户账号管理', icon: 'User', roles: ['admin', 'jw'] }
      },
      {
        path: 'system/log',
        name: 'Log',
        component: () => import('../views/system/Log.vue'),
        meta: { title: '系统操作日志', icon: 'Document', roles: ['admin'] }
      },
      {
        path: 'system/notices',
        name: 'Notices',
        component: () => import('../views/system/Notice.vue'),
        meta: { title: '通知公告管理', icon: 'Bell', roles: ['admin', 'jw'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  const isLoggedIn = userStore.isLoggedIn;
  const userRole = userStore.role;

  // 1. 登录校验
  if (to.matched.some(record => record.meta.requiresAuth !== false) && !isLoggedIn) {
    next('/login');
  } 
  // 2. 已登录防重返登录页
  else if (to.path === '/login' && isLoggedIn) {
    next('/');
  } 
  // 3. 角色权限校验
  else if (to.meta.roles && !to.meta.roles.includes(userRole)) {
    // 权限不足重定向到首页，但如果是进入首页/dashboard时权限不足（比如缓存损坏导致 role 丢失），则退回登录页以防止无限循环卡死
    if (to.path === '/dashboard' || to.path === '/') {
      userStore.logout();
      next('/login');
    } else {
      next('/dashboard');
    }
  } else {
    next();
  }
});

export default router;
