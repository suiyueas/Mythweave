package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.NovelSentinelCheckLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelSentinelCheckLogMapper extends BaseMapper<NovelSentinelCheckLog> {
    List<NovelSentinelCheckLog> selectRecent(Long projectId, int limit);
    int countRunning(Long projectId);
}