package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.NovelAiConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NovelAiConfigMapper extends BaseMapper<NovelAiConfig> {
    NovelAiConfig selectByProjectId(Long projectId);
}