package com.mythweave.web.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体封装类
 * 
 * 所有API接口的响应都使用此格式，确保前端有统一的响应结构
 * 
 * 响应状态码约定：
 * - 200: 成功
 * - 400: 请求参数错误
 * - 404: 资源不存在
 * - 500: 服务器内部错误
 * 
 * @param <T> 响应数据的泛型类型
 */
@Data
public class R<T> implements Serializable {
    /** 状态码：200成功，非200表示错误 */
    private int code;
    
    /** 响应消息：成功时返回"success"，错误时返回具体错误信息 */
    private String message;
    
    /** 响应数据：根据不同接口返回不同类型的数据 */
    private T data;
    
    /** 响应时间戳（毫秒），用于日志追踪和缓存管理 */
    private long timestamp;

    /**
     * 私有构造函数，使用静态工厂方法创建实例
     */
    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建成功响应（带数据）
     * @param data 响应数据
     * @return 成功响应对象
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    /**
     * 创建成功响应（不带数据）
     * @return 成功响应对象
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /**
     * 创建失败响应（自定义状态码）
     * @param code 错误状态码
     * @param message 错误信息
     * @return 失败响应对象
     */
    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    /**
     * 创建失败响应（默认500状态码）
     * @param message 错误信息
     * @return 失败响应对象
     */
    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }

    /**
     * 创建404资源不存在响应
     * @param message 错误信息
     * @return 404响应对象
     */
    public static <T> R<T> notFound(String message) {
        return fail(404, message);
    }

    /**
     * 创建400请求参数错误响应
     * @param message 错误信息
     * @return 400响应对象
     */
    public static <T> R<T> badRequest(String message) {
        return fail(400, message);
    }
}