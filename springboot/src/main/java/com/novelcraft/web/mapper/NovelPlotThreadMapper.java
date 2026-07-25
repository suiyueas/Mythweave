package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface NovelPlotThreadMapper extends BaseMapper<NovelPlotThread> {
    @Select("SELECT * FROM novel_plot_thread WHERE project_id = #{projectId} AND deleted = 0")
    List<NovelPlotThread> selectByProjectId(@Param("projectId") Long projectId);
}
