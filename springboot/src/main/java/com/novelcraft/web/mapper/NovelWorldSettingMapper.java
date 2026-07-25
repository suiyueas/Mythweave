package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelWorldSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface NovelWorldSettingMapper extends BaseMapper<NovelWorldSetting> {
    @Select("SELECT * FROM novel_world_setting WHERE project_id = #{projectId} AND deleted = 0 ORDER BY level")
    List<NovelWorldSetting> selectByProjectId(@Param("projectId") Long projectId);
}
