package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.NovelForeshadowing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NovelForeshadowingMapper extends BaseMapper<NovelForeshadowing> {
    List<NovelForeshadowing> selectByProjectId(Long projectId);
    List<NovelForeshadowing> selectPendingByProject(Long projectId);
    List<NovelForeshadowing> selectUrgentByProject(Long projectId, Integer currentChapter);
    List<NovelForeshadowing> selectRelatedByChapterId(Long projectId, Long chapterId);

    /** 回退回收章节被删除的伏笔（resolved → pending） */
    int revertResolvedByChapter(@Param("projectId") Long projectId, @Param("chapterId") Long chapterId);

    /** 自愈：回收标记指向不存在章节的伏笔回退为待回收（兼容历史错误数据） */
    int healOrphanResolved(@Param("projectId") Long projectId);
}