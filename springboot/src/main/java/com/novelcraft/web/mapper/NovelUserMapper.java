package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NovelUserMapper extends BaseMapper<NovelUser> {
    @Select("SELECT * FROM novel_user WHERE username = #{username} AND deleted = 0")
    NovelUser selectByUsername(@Param("username") String username);
}