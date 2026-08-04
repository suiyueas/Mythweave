package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.NovelProject;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelProjectMapper extends BaseMapper<NovelProject> {
    int deletePhysically(Long id);
    List<NovelProject> selectByUserId(Long userId);
    List<Long> selectActiveProjectIdsWithinDays(int days);
}