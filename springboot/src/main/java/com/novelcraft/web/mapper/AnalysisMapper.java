package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelAnalysis;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AnalysisMapper extends BaseMapper<NovelAnalysis> {
    List<NovelAnalysis> findByProjectIdOrderByCreateTimeDesc(Long projectId);
    List<NovelAnalysis> findByProjectIdAndChapterIdOrderByCreateTimeDesc(Long projectId, Long chapterId);
    void deleteByProjectId(Long projectId);
}