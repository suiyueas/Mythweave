package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelAiSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelAiSessionMapper extends BaseMapper<NovelAiSession> {
    List<NovelAiSession> selectByProjectId(Long projectId);
    List<NovelAiSession> selectDistinctSessions(Long projectId);
    List<NovelAiSession> selectBySessionId(Long sessionId);
}