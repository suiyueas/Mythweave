package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelSentinelCheckLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NovelSentinelCheckLogMapper extends BaseMapper<NovelSentinelCheckLog> {

    @Select("SELECT * FROM novel_sentinel_check_log WHERE project_id = #{projectId} AND deleted = 0 ORDER BY started_at DESC LIMIT #{limit}")
    List<NovelSentinelCheckLog> selectRecent(@Param("projectId") Long projectId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM novel_sentinel_check_log WHERE project_id = #{projectId} AND status = 'running' AND deleted = 0")
    int countRunning(@Param("projectId") Long projectId);
}
