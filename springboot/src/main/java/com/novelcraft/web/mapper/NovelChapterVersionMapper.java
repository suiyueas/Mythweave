package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelChapterVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NovelChapterVersionMapper extends BaseMapper<NovelChapterVersion> {

    @Select("SELECT * FROM novel_chapter_version WHERE chapter_id = #{chapterId} AND deleted = 0 ORDER BY create_time DESC")
    List<NovelChapterVersion> selectByChapterId(@Param("chapterId") Long chapterId);
}
