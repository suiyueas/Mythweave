package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelInspiration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface NovelInspirationMapper extends BaseMapper<NovelInspiration> {
    @Select("SELECT * FROM novel_inspiration WHERE project_id = #{projectId} AND deleted = 0 ORDER BY create_time DESC")
    List<NovelInspiration> selectByProjectId(@Param("projectId") Long projectId);
}
