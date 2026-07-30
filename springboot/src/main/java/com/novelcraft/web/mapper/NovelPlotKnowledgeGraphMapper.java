package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelPlotKnowledgeGraph;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelPlotKnowledgeGraphMapper extends BaseMapper<NovelPlotKnowledgeGraph> {
    List<NovelPlotKnowledgeGraph> selectByProjectId(Long projectId);
}