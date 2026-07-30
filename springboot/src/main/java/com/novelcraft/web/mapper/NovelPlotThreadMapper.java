package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelPlotThread;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelPlotThreadMapper extends BaseMapper<NovelPlotThread> {
    List<NovelPlotThread> selectByProjectId(Long projectId);
}