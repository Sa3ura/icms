package com.icms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icms.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    /**
     * 原子地把 userId 追加到通知 read_by JSON 数组，避免"读-改-写"竞态：
     *   两个并发请求同时读旧数组、各自 add 后写回，会把对方的写入覆盖掉。
     * 用 MySQL 8 的 JSON_ARRAY_APPEND + JSON_CONTAINS 一条 SQL 完成幂等追加。
     * COALESCE 兜住旧数据里 read_by 可能为 NULL 的行。
     * deleted = 0 手动兼容逻辑删除（原生 @Update 不走 MP 自动注入）。
     *
     * @return 影响行数：1=本次新标记，0=已读 or 不存在 or 已删除
     */
    @Update("UPDATE notice " +
            "SET read_by = JSON_ARRAY_APPEND(COALESCE(read_by, JSON_ARRAY()), '$', #{userId}) " +
            "WHERE id = #{id} " +
            "AND NOT JSON_CONTAINS(COALESCE(read_by, JSON_ARRAY()), CAST(#{userId} AS JSON), '$') " +
            "AND deleted = 0")
    int markReadAtomic(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 批量原子标记：仅命中当前角色可见的通知（target_role 为空 = 广播，否则需匹配传入 role）。
     * 幂等：已读通知 JSON_CONTAINS 会过滤掉，不会重复追加。
     *
     * @param userId 当前登录用户 id
     * @param role   当前登录用户角色；null 时仅命中广播通知
     * @return 本次新标记的通知条数
     */
    @Update("UPDATE notice " +
            "SET read_by = JSON_ARRAY_APPEND(COALESCE(read_by, JSON_ARRAY()), '$', #{userId}) " +
            "WHERE (target_role IS NULL OR target_role = '' OR target_role = #{role}) " +
            "AND NOT JSON_CONTAINS(COALESCE(read_by, JSON_ARRAY()), CAST(#{userId} AS JSON), '$') " +
            "AND deleted = 0")
    int markAllReadAtomic(@Param("userId") Long userId, @Param("role") String role);
}
