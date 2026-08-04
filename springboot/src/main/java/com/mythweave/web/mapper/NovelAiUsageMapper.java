package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.NovelAiUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NovelAiUsageMapper extends BaseMapper<NovelAiUsage> {
    
    @Select("SELECT * FROM novel_ai_usage WHERE project_id = #{projectId} AND deleted = 0 LIMIT 1")
    NovelAiUsage selectByProjectId(Long projectId);
}