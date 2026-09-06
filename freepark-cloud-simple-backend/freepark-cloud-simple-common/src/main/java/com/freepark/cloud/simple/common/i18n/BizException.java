package com.freepark.cloud.simple.common.i18n;

/**
 * 携带消息 key 的业务异常，由上层解析为本地化文案。
 */
public class BizException extends RuntimeException {

    private final int code;
    private final String messageKey;

    public BizException(int code, String messageKey) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
    }

    public int getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
