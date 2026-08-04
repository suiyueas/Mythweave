package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.NovelWorldSetting;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelWorldSettingMapper extends BaseMapper<NovelWorldSetting> {
    List<NovelWorldSetting> selectByProjectId(Long projectId);
}