package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_context_snapshot")
public class NovelContextSnapshot extends BaseEntity {
    private Long projectId;
    private Long chapterId;
    private String contextType;
    private String queryText;
    private String assembledPrompt;
    private Integer tokensUsed;
}
