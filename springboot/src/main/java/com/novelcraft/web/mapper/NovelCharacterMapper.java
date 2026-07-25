package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelCharacter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface NovelCharacterMapper extends BaseMapper<NovelCharacter> {
    @Select("SELECT * FROM novel_character WHERE project_id = #{projectId} AND deleted = 0")
    List<NovelCharacter> selectByProjectId(@Param("projectId") Long projectId);
}
