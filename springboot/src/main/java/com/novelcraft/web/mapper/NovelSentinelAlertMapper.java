package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelSentinelAlert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelSentinelAlertMapper extends BaseMapper<NovelSentinelAlert> {
    List<NovelSentinelAlert> selectByProjectId(Long projectId);
    int countByProject(Long projectId);
    int countUnresolved(Long projectId);
    int countByType(Long projectId, String type);
    int countUnresolvedByType(Long projectId, String type);
    List<NovelSentinelAlert> selectUnresolved(Long projectId);
}