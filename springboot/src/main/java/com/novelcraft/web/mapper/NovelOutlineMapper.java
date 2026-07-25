package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelOutline;
import com.novelcraft.web.model.BatchSortActItem;
import com.novelcraft.web.model.BatchSortItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NovelOutlineMapper extends BaseMapper<NovelOutline> {

    @Select("SELECT * FROM novel_outline WHERE project_id = #{projectId} AND deleted = 0 ORDER BY sort_order")
    List<NovelOutline> selectByProjectId(@Param("projectId") Long projectId);

    /** 批量更新排序（拖拽落地） */
    int updateBatchSort(@Param("items") List<BatchSortItem> items);

    /** 批量更新状态 */
    int updateBatchStatus(@Param("projectId") Long projectId, @Param("ids") List<Long> ids, @Param("status") String status);

    /** 批量删除 */
    int deleteBatch(@Param("projectId") Long projectId, @Param("ids") List<Long> ids);

    /** 批量更新排序和幕归属（跨幕拖拽） */
    int updateBatchSortAndAct(@Param("items") List<BatchSortActItem> items);
}
