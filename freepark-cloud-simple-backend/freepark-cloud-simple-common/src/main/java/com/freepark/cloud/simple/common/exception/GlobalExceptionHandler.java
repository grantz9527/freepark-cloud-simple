package com.freepark.cloud.simple.common.exception;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.i18n.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：按当前语言解析消息并统一转换为 ApiResult 结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageService messageService;

    public GlobalExceptionHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBiz(BizException e) {
        Object[] args = e.getArgs();
        String message = args == null ? messageService.get(e.getMessageKey())
                : messageService.get(e.getMessageKey(), args);
        return ApiResult.fail(e.getCode(), message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResult.fail(400, messageService.get(MessageKeys.COMMON_BAD_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ApiResult.fail(500, messageService.get(MessageKeys.COMMON_SERVER_ERROR));
    }
}
