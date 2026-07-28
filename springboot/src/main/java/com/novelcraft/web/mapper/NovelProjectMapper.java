package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelProject;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NovelProjectMapper extends BaseMapper<NovelProject> {

    /** 物理删除（绕过 @TableLogic 软删除） */
    @Delete("DELETE FROM novel_project WHERE id = #{id}")
    int deletePhysically(@Param("id") Long id);

    /** 按用户ID查询作品列表 */
    @Select("SELECT * FROM novel_project WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<NovelProject> selectByUserId(@Param("userId") Long userId);

    /** 查询最近N天内有更新活动的项目ID列表（用于缓存预热） */
    @Select("SELECT DISTINCT cp.id FROM novel_project cp " +
            "INNER JOIN novel_chapter cc ON cp.id = cc.project_id " +
            "WHERE cc.update_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "AND cp.deleted = 0 AND cc.deleted = 0")
    List<Long> selectActiveProjectIdsWithinDays(@Param("days") int days);
}