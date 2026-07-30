package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelInspiration;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelInspirationMapper extends BaseMapper<NovelInspiration> {
    List<NovelInspiration> selectByProjectId(Long projectId);
}