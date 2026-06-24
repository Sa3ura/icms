import { defineStore } from 'pinia';
import { apiLogin } from '../api';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('icms_token') || '',
    userInfo: JSON.parse(localStorage.getItem('icms_user_info') || 'null'),
    theme: localStorage.getItem('icms_theme') || 'light'
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.userInfo?.user_type || 'guest',
    realName: (state) => state.userInfo?.real_name || '访客'
  },
  actions: {
    async login(username, password) {
      const res = await apiLogin(username, password);
      if (res.code === 200) {
        this.token = res.data.token;
        this.userInfo = res.data.userInfo;
        localStorage.setItem('icms_token', this.token);
        localStorage.setItem('icms_user_info', JSON.stringify(this.userInfo));
        return res;
      }
      throw new Error(res.message);
    },
    logout() {
      this.token = '';
      this.userInfo = null;
      localStorage.removeItem('icms_token');
      localStorage.removeItem('icms_user_info');
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
