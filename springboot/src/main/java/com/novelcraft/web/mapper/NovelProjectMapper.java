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
}