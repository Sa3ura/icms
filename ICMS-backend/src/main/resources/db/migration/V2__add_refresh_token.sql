-- ============================================================
-- ICMS V2 — 添加 refresh_token 列（双 Token 刷新模式）
-- ============================================================
ALTER TABLE sys_user ADD COLUMN refresh_token VARCHAR(64) NULL AFTER password;
