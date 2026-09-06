package com.freepark.cloud.simple.common.web;

import java.util.List;

/**
 * 通用分页结果。
 */
public record PageResult<T>(List<T> list, long total, int page, int size) {

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        return new PageResult<>(list, total, page, size);
    }
}
