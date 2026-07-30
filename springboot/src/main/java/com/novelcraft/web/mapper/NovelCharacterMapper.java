package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelCharacter;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelCharacterMapper extends BaseMapper<NovelCharacter> {
    List<NovelCharacter> selectByProjectId(Long projectId);
}