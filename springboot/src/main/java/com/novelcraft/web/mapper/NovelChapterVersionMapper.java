package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelChapterVersion;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NovelChapterVersionMapper extends BaseMapper<NovelChapterVersion> {
    List<NovelChapterVersion> selectByChapterId(Long chapterId);
}