package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface NovelChapterMapper extends BaseMapper<NovelChapter> {
    List<NovelChapter> selectByProjectId(@Param("projectId") Long projectId);
    List<NovelChapter> selectByVolumeId(@Param("volumeId") Long volumeId);
    NovelChapter selectByIdWithDeleted(@Param("id") Long id);
    int markDeletedById(@Param("id") Long id);
    Integer countByProject(@Param("projectId") Long projectId);
    List<NovelChapter> selectRecentByProject(@Param("projectId") Long projectId, @Param("limit") int limit);
    Integer sumWordCountByProject(@Param("projectId") Long projectId);
    Integer selectMaxSortOrder(@Param("projectId") Long projectId);
    Integer getMaxSortOrder(@Param("projectId") Long projectId);
    NovelChapter selectBySortOrder(@Param("projectId") Long projectId, @Param("sortOrder") int sortOrder);
    Integer sumWordCountByProjectAndDate(@Param("projectId") Long projectId, @Param("date") LocalDate date);
    Integer sumWordCountByProjectAndDateRange(@Param("projectId") Long projectId, @Param("start") LocalDate start, @Param("end") LocalDate end);
    List<NovelChapter> selectPublishedChapters(@Param("projectId") Long projectId);
    List<NovelChapter> selectChaptersMentioningCharacter(@Param("projectId") Long projectId, @Param("characterId") Long characterId);
    List<NovelChapter> selectNearbyChaptersForIncremental(@Param("projectId") Long projectId, @Param("currentOrder") Integer currentOrder, @Param("range") int range);
}