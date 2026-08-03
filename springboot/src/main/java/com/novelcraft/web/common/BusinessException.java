package com.novelcraft.web.common;

import lombok.Getter;

/**
 * 业务异常
 * 
 * 用于在业务逻辑处理中抛出异常，如：
 * - 数据不存在
 * - 权限不足
 * - 业务规则校验失败
 * - 状态不符合预期
 * 
 * 区别于系统异常（如数据库异常、网络异常等）
 * 业务异常是预期的错误情况，通常由用户操作或数据状态引起
 * 
 * 异常会被GlobalExceptionHandler统一捕获并转换为HTTP响应
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /** 错误状态码（用于前端区分不同类型的错误） */
    private final int code;

    /**
     * 创建业务异常
     * @param code 错误状态码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 创建业务异常（默认500状态码）
     * @param message 错误信息
     */
    public BusinessException(String message) {
        this(500, message);
    }
}