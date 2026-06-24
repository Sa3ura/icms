import { defineStore } from 'pinia';
import { apiLogin, setToken, request } from '../api';

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: USE_MOCK ? localStorage.getItem('icms_token') : null,
    userInfo: JSON.parse(localStorage.getItem('icms_user_info') || 'null'),
    theme: localStorage.getItem('icms_theme') || 'light'
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.userInfo?.user_type || 'guest',
    realName: (state) => state.userInfo?.real_name || '访客'
  },
  actions: {
    async login(username, password, captchaId, captchaCode) {
      const res = await apiLogin(username, password, captchaId, captchaCode);
      if (res.code === 200) {
        this.token = res.data.token;
        this.userInfo = res.data.userInfo;
        setToken(res.data.token);
        localStorage.setItem('icms_user_info', JSON.stringify(this.userInfo));
        if (USE_MOCK) {
          localStorage.setItem('icms_token', this.token);
        }
        return res;
      }
      throw new Error(res.message);
    },
    async logout() {
      if (!USE_MOCK) {
        try { await request.post('/api/auth/logout'); } catch { /* ignore */ }
      }
      this.token = null;
      this.userInfo = null;
      setToken(null);
      localStorage.removeItem('icms_token');
      localStorage.removeItem('icms_user_info');
    },
    async tryRefresh() {
      if (USE_MOCK) {
        const saved = localStorage.getItem('icms_token');
        if (saved) {
          this.token = saved;
          setToken(saved);
          return true;
        }
        return false;
      }
      try {
        const res = await request.post('/api/auth/refresh');
        if (res.code === 200 && res.data.token) {
          this.token = res.data.token;
          this.userInfo = res.data.userInfo;
          localStorage.setItem('icms_user_info', JSON.stringify(this.userInfo));
          return true;
        }
      } catch { /* refresh failed */ }
      return false;
    },
    toggleTheme() {
      this.theme = this.theme === 'light' ? 'dark' : 'light';
      localStorage.setItem('icms_theme', this.theme);

      const root = document.documentElement;
      root.classList.add('theme-transitioning');

      if (this.theme === 'dark') {
        root.classList.add('dark');
      } else {
        root.classList.remove('dark');
      }

      setTimeout(() => {
        root.classList.remove('theme-transitioning');
      }, 400);
    },
    initTheme() {
      if (this.theme === 'dark') {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    }
  }
});
