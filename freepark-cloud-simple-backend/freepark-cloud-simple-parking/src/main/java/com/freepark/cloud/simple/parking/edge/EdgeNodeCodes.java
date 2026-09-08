package com.freepark.cloud.simple.parking.edge;

import com.freepark.cloud.simple.parking.entity.EdgeNode;

/**
 * 边缘节点编号的主题安全校验：节点编号会拼进 MQTT 主题（{前缀}/{编号} 与
 * {心跳主题}/{编号}），必须满足“无空白、无通配符 #/+、不含斜杠、长度合规”。
 */
public final class EdgeNodeCodes {

    private EdgeNodeCodes() {
    }

    public static boolean isTopicSafe(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        if (code.length() > EdgeNode.MAX_CODE_LENGTH) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '/' || c == '#' || c == '+' || c == '\u0000' || Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }
}
