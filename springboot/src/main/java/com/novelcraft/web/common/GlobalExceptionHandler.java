package com.novelcraft.web.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 * 
 * 统一捕获并处理所有Controller层抛出的异常，转换为统一的响应格式
 * 
 * 支持的异常类型：
 * - BusinessException: 业务逻辑异常（自定义）
 * - IllegalArgumentException: 参数非法异常
 * - MethodArgumentNotValidException: 请求体参数校验失败
 * - ConstraintViolationException: 方法参数约束校验失败
 * - HttpMessageNotReadableException: 请求体格式错误
 * - MissingServletRequestParameterException: 缺少必填参数
 * - MethodArgumentTypeMismatchException: 参数类型不匹配
 * - Exception: 其他未处理异常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务逻辑异常
     * 通常用于业务规则校验失败、数据不存在等业务场景
     * 
     * @param e 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数非法异常
     * 通常用于参数格式校验失败、范围校验失败等场景
     * 
     * @param e 参数非法异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.badRequest(e.getMessage());
    }

    /**
     * 处理请求体参数校验失败
     * 当使用@Valid或@Validated注解且请求体不符合约束条件时触发
     * 例如：@NotBlank、@NotNull、@Size等注解校验失败
     * 
     * @param e 方法参数校验异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError error = e.getBindingResult().getFieldError();
        String message = error != null ? error.getDefaultMessage() : "请求参数校验失败";
        log.warn("参数校验失败: {}", message);
        return R.badRequest(message);
    }

    /**
     * 处理方法参数约束校验失败
     * 当使用@Validated注解在方法参数（如@RequestParam、@PathVariable）上约束校验失败时触发
     * 
     * @param e 约束校验异常
     * @return 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("参数约束校验失败: {}", e.getMessage());
        return R.badRequest(e.getMessage());
    }

    /**
     * 处理请求体格式错误
     * 当请求体为空或JSON格式不正确时触发
     * 
     * @param e HTTP消息不可读异常
     * @return 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return R.badRequest("请求体缺失或格式错误");
    }

    /**
     * 处理缺少必填请求参数
     * 当@RequestParam标注的必填参数缺失时触发
     * 
     * @param e 缺少参数异常
     * @return 错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.badRequest("缺少必填参数: " + e.getParameterName());
    }

    /**
     * 处理路径/查询参数类型不匹配
     * 当参数类型不匹配时触发，例如：期望Integer但收到String
     * 
     * @param e 参数类型不匹配异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.badRequest("参数 " + e.getName() + " 类型错误");
    }

    /**
     * 处理所有未捕获的异常
     * 作为最后的异常处理兜底，防止未处理的异常直接暴露给用户
     * 
     * @param e 其他异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail("服务器内部错误");
    }
}