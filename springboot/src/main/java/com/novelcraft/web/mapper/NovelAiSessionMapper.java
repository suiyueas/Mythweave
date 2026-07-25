package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelAiSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface NovelAiSessionMapper extends BaseMapper<NovelAiSession> {
    @Select("SELECT * FROM novel_ai_session WHERE project_id = #{projectId} AND deleted = 0 ORDER BY create_time DESC")
    List<NovelAiSession> selectByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT DISTINCT session_id, MIN(create_time) AS create_time FROM novel_ai_session WHERE project_id = #{projectId} AND session_id IS NOT NULL AND deleted = 0 GROUP BY session_id ORDER BY MIN(create_time) DESC")
    List<NovelAiSession> selectDistinctSessions(@Param("projectId") Long projectId);

    @Select("SELECT * FROM novel_ai_session WHERE session_id = #{sessionId} AND deleted = 0 ORDER BY create_time ASC")
    List<NovelAiSession> selectBySessionId(@Param("sessionId") Long sessionId);
}
