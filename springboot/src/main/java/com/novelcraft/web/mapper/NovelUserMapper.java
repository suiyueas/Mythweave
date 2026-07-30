package com.novelcraft.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelcraft.web.entity.NovelUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NovelUserMapper extends BaseMapper<NovelUser> {
    NovelUser selectByUsername(String username);
}