package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelAiConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NovelAiConfigMapper extends BaseMapper<NovelAiConfig> {
    NovelAiConfig selectByProjectId(Long projectId);
}