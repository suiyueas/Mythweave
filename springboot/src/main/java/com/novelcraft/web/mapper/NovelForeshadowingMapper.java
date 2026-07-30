package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelForeshadowing;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelForeshadowingMapper extends BaseMapper<NovelForeshadowing> {
    List<NovelForeshadowing> selectByProjectId(Long projectId);
    List<NovelForeshadowing> selectPendingByProject(Long projectId);
    List<NovelForeshadowing> selectUrgentByProject(Long projectId, Integer currentChapter);
    List<NovelForeshadowing> selectRelatedByChapterId(Long projectId, Long chapterId);
}