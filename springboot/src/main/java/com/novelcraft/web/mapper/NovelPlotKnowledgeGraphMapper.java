package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelPlotKnowledgeGraph;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface NovelPlotKnowledgeGraphMapper extends BaseMapper<NovelPlotKnowledgeGraph> {
    @Select("SELECT * FROM novel_plot_knowledge_graph WHERE project_id = #{projectId} AND deleted = 0")
    List<NovelPlotKnowledgeGraph> selectByProjectId(@Param("projectId") Long projectId);
}
