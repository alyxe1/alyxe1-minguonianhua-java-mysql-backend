package org.exh.nianhuawechatminiprogrambackend.handler;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 - URL:{}, Code:{}, Message:{}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // 处理参数绑定异常（包括@Valid注解校验失败）
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public Result<Void> handleBindException(Exception e) {
        String errorMessage = "参数校验失败";
        if (e instanceof BindException) {
            BindingResult bindingResult = ((BindException) e).getBindingResult();
            if (bindingResult.hasErrors()) {
                errorMessage = bindingResult.getFieldError().getDefaultMessage();
            }
        } else if (e instanceof MethodArgumentNotValidException) {
            BindingResult bindingResult = ((MethodArgumentNotValidException) e).getBindingResult();
            if (bindingResult.hasErrors()) {
                errorMessage = bindingResult.getFieldError().getDefaultMessage();
            }
        }
        log.warn("参数绑定异常: {}", errorMessage);
        return Result.error(errorMessage);
    }

    // 处理请求绑定异常（缺少参数等）
    @ExceptionHandler(ServletRequestBindingException.class)
    public Result<Void> handleServletRequestBindingException(ServletRequestBindingException e, HttpServletRequest request) {
        log.warn("请求绑定异常 - URL:{}, Message:{}", request.getRequestURI(), e.getMessage());
        return Result.error("请求参数错误: " + e.getMessage());
    }

    // 处理空指针异常
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 - URL:{}, Message:{}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("服务器内部错误");
    }

    // 处理所有未捕获的异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常 - URL:{}, Message:{}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("服务器内部错误");
    }
}
