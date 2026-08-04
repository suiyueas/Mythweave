package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.NovelUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NovelUserMapper extends BaseMapper<NovelUser> {
    NovelUser selectByUsername(String username);
}