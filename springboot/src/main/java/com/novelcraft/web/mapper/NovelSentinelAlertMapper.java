package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelSentinelAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface NovelSentinelAlertMapper extends BaseMapper<NovelSentinelAlert> {

    @Select("SELECT * FROM novel_sentinel_alert WHERE project_id = #{projectId} AND deleted = 0 ORDER BY create_time DESC")
    List<NovelSentinelAlert> selectByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM novel_sentinel_alert WHERE project_id = #{projectId} AND deleted = 0")
    int countByProject(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM novel_sentinel_alert WHERE project_id = #{projectId} AND resolved = 0 AND deleted = 0")
    int countUnresolved(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM novel_sentinel_alert WHERE project_id = #{projectId} AND type = #{type} AND deleted = 0")
    int countByType(@Param("projectId") Long projectId, @Param("type") String type);

    @Select("SELECT COUNT(*) FROM novel_sentinel_alert WHERE project_id = #{projectId} AND type = #{type} AND resolved = 0 AND deleted = 0")
    int countUnresolvedByType(@Param("projectId") Long projectId, @Param("type") String type);

    @Select("SELECT * FROM novel_sentinel_alert WHERE project_id = #{projectId} AND resolved = 0 AND deleted = 0 ORDER BY severity DESC, create_time DESC")
    List<NovelSentinelAlert> selectUnresolved(@Param("projectId") Long projectId);
}
