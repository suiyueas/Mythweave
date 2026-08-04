package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.NovelOutline;
import com.mythweave.web.model.BatchSortActItem;
import com.mythweave.web.model.BatchSortItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NovelOutlineMapper extends BaseMapper<NovelOutline> {
    List<NovelOutline> selectByProjectId(@Param("projectId") Long projectId);
    int updateBatchSort(@Param("items") List<BatchSortItem> items);
    int updateBatchStatus(@Param("projectId") Long projectId, @Param("ids") List<Long> ids, @Param("status") String status);
    int deleteBatch(@Param("projectId") Long projectId, @Param("ids") List<Long> ids);
    int updateBatchSortAndAct(@Param("items") List<BatchSortActItem> items);
}