package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelAiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NovelAiConfigMapper extends BaseMapper<NovelAiConfig> {
    @Select("SELECT * FROM novel_ai_config WHERE project_id = #{projectId} AND deleted = 0 LIMIT 1")
    NovelAiConfig selectByProjectId(@Param("projectId") Long projectId);
}
