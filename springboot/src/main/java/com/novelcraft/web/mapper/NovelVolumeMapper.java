package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelVolume;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelVolumeMapper extends BaseMapper<NovelVolume> {
    List<NovelVolume> selectByProjectId(Long projectId);
}