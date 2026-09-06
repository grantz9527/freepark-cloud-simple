package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreatePatternAllowlistRequest;
import com.freepark.cloud.simple.parking.dto.PatternAllowlistView;
import com.freepark.cloud.simple.parking.dto.UpdatePatternAllowlistRequest;
import com.freepark.cloud.simple.parking.service.PatternAllowlistService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 放行名单（车牌号段规则）管理。
 */
@RestController
@RequestMapping("/api/lots/{lotId}/pattern-allowlist")
public class PatternAllowlistController {

    private final PatternAllowlistService patternAllowlistService;

    public PatternAllowlistController(PatternAllowlistService patternAllowlistService) {
        this.patternAllowlistService = patternAllowlistService;
    }

    @GetMapping
    public ApiResult<PageResult<PatternAllowlistView>> list(@PathVariable Long lotId,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(patternAllowlistService.listEntries(lotId, keyword, page, size));
    }

    @PostMapping
    public ApiResult<PatternAllowlistView> create(@PathVariable Long lotId,
                                                  @RequestBody CreatePatternAllowlistRequest request) {
        return ApiResult.ok(patternAllowlistService.createEntry(lotId, request));
    }

    @PutMapping("/{entryId}")
    public ApiResult<PatternAllowlistView> update(@PathVariable Long lotId,
                                                  @PathVariable Long entryId,
                                                  @RequestBody UpdatePatternAllowlistRequest request) {
        return ApiResult.ok(patternAllowlistService.updateEntry(lotId, entryId, request));
    }

    @DeleteMapping("/{entryId}")
    public ApiResult<Void> delete(@PathVariable Long lotId,
                                  @PathVariable Long entryId) {
        patternAllowlistService.deleteEntry(lotId, entryId);
        return ApiResult.ok();
    }
}
