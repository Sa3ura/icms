<template>
  <footer class="icp-footer">
    <a
      href="https://beian.miit.gov.cn/"
      target="_blank"
      rel="noopener noreferrer"
    >湘ICP备2026047734号</a>
  </footer>
</template>

<style scoped>
.icp-footer {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  padding: 12px 24px max(12px, env(safe-area-inset-bottom, 0px));
  font-size: 12px;
  line-height: 20px;
}

.icp-footer a {
  color: var(--text-regular);
  text-decoration: none;
}

.icp-footer a:hover,
.icp-footer a:focus-visible {
  color: var(--text-main);
  text-decoration: underline;
}
</style>
