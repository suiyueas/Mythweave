package com.mythweave.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mythweave.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 用户统计
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_user_stats")
public class NovelUserStats extends BaseEntity {
    private Long userId;
    private Integer totalWords;
    private Integer continuousDays;
    private Integer worksCount;
    private Integer userLevel;
    private LocalDate lastWriteDate;
}
