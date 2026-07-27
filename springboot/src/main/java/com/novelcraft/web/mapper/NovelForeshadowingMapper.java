package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelForeshadowing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface NovelForeshadowingMapper extends BaseMapper<NovelForeshadowing> {

    @Select("SELECT * FROM novel_foreshadowing WHERE project_id = #{projectId} AND deleted = 0")
    List<NovelForeshadowing> selectByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM novel_foreshadowing WHERE project_id = #{projectId} AND status != 'resolved' AND deleted = 0 ORDER BY chapter_id")
    List<NovelForeshadowing> selectPendingByProject(@Param("projectId") Long projectId);

    @Select("SELECT * FROM novel_foreshadowing WHERE project_id = #{projectId} AND status != 'resolved' AND deleted = 0 AND chapter_id <= #{currentChapter} ORDER BY chapter_id ASC")
    List<NovelForeshadowing> selectUrgentByProject(@Param("projectId") Long projectId, @Param("currentChapter") Integer currentChapter);
}