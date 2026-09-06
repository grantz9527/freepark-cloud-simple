package com.freepark.cloud.simple.common;

/**
 * 统一接口响应结构。
 *
 * <p>key 为可选业务标识（i18n key），供前端按语言翻译；message 为后端兜底文案。</p>
 */
public class ApiResult<T> {

    /** 200 成功，其余为业务错误码 */
    private int code;
    private String message;
    /** 可选：业务错误 i18n key */
    private String key;
    private T data;

    public ApiResult() {
    }

    public ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(200, "ok", data);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(200, "ok", null);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    public static <T> ApiResult<T> fail(int code, String message, String key) {
        ApiResult<T> result = new ApiResult<>(code, message, null);
        result.key = key;
        return result;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
