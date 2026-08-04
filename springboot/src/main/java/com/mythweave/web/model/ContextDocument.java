package com.mythweave.web.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * ES 全书内容向量索引文档
 */
@Data
@Document(indexName = "novel_context")
public class ContextDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long novelId;

    @Field(type = FieldType.Long)
    private Long chapterId;

    /** 内容类型: paragraph / character / world_setting / glossary / outline */
    @Field(type = FieldType.Keyword)
    private String chunkType;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String chunkText;

    /** 1024维向量 */
    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private double[] embedding;

    @Field(type = FieldType.Integer)
    private Integer chunkSeq;

    /** 附加元数据 */
    @Field(type = FieldType.Text)
    private String metadata;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
}
