package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体类
 * 
 * 对应数据库表：novel_character
 * 
 * 角色是作品中的重要元素，每个角色包含：
 * - 基本信息：姓名、年龄、角色定位（主角/配角/反派等）
 * - 外貌特征：头像颜色等
 * - 性格描述：性格特点、情感特征
 * - 能力值：战斗力、智慧、情感、魅力等评分
 * - 关系网络：与其他角色的关系
 * - 角色弧线：角色发展轨迹（起点、终点、进度）
 * - 最后出现位置：章节/场景位置
 * 
 * 支持角色弧线进度追踪，用于AI辅助角色发展
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_character")
public class NovelCharacter extends BaseEntity {
    /** 所属作品ID */
    private Long projectId;
    
    /** 角色名称 */
    @NotBlank(message = "人物名称不能为空")
    private String name;
    
    /** 角色定位（protagonist-主角/antagonist-反派/supporting-配角/minor-龙套） */
    private String role;
    
    /** 角色类型（如战士、法师、普通人等） */
    private String type;
    
    /** 年龄 */
    private Integer age;
    
    /** 头像颜色（用于前端展示时的默认头像背景色） */
    private String avatarColor;
    
    /** 角色描述/简介 */
    private String description;
    
    /** 性格特点 */
    private String personality;
    
    /** 与其他角色的关系描述 */
    private String relation;
    
    /** 角色弧线起点/初始状态 */
    private String arcStart;
    
    /** 角色弧线终点/目标状态 */
    private String arcEnd;
    
    /** 角色弧线进度（0-100） */
    private Integer arcProgress;
    
    /** 战斗力评分（1-100） */
    private Integer combat;
    
    /** 智慧评分（1-100） */
    private Integer wisdom;
    
    /** 情感评分（1-100） */
    private Integer emotion;
    
    /** 魅力评分（1-100） */
    private Integer charm;
    
    /** 最后出现位置（章节ID或场景描述） */
    private String lastSeen;
}