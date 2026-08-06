package com.mythweave.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mythweave.web.entity.NovelUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 小说用户 Mapper
 * 提供用户数据的数据库查询操作
 */
@Mapper
public interface NovelUserMapper extends BaseMapper<NovelUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户对象，不存在返回 null
     */
    NovelUser selectByUsername(String username);

    /**
     * 根据邮箱查询用户（支持邮箱登录）
     *
     * @param email 邮箱地址
     * @return 用户对象，不存在返回 null
     */
    @Select("SELECT * FROM novel_user WHERE email = #{email} LIMIT 1")
    NovelUser selectByEmail(String email);
}