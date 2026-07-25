package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelVolume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NovelVolumeMapper extends BaseMapper<NovelVolume> {

    @Select("SELECT * FROM novel_volume WHERE project_id = #{projectId} AND deleted = 0 ORDER BY sort_order")
    List<NovelVolume> selectByProjectId(@Param("projectId") Long projectId);
}
