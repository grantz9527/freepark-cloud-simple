package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.parking.dto.CreateEdgeNodeRequest;
import com.freepark.cloud.simple.parking.dto.EdgeNodeView;
import com.freepark.cloud.simple.parking.dto.UpdateEdgeNodeLotsRequest;
import com.freepark.cloud.simple.parking.dto.UpdateEdgeNodeRequest;
import com.freepark.cloud.simple.parking.service.EdgeNodeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 边缘节点管理：节点资料维护与“节点 ↔ 车场”绑定（页面入口属系统管理菜单，仅超级管理员）。
 */
@RestController
@RequestMapping("/api/system/edge-nodes")
public class EdgeNodeController {

    private final EdgeNodeService nodeService;

    public EdgeNodeController(EdgeNodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping
    public ApiResult<List<EdgeNodeView>> list() {
        return ApiResult.ok(nodeService.listNodes());
    }

    @PostMapping
    public ApiResult<EdgeNodeView> create(@RequestBody CreateEdgeNodeRequest request) {
        return ApiResult.ok(nodeService.createNode(request));
    }

    @PutMapping("/{nodeId}")
    public ApiResult<EdgeNodeView> update(@PathVariable Long nodeId,
                                          @RequestBody UpdateEdgeNodeRequest request) {
        return ApiResult.ok(nodeService.updateNode(nodeId, request));
    }

    @PutMapping("/{nodeId}/lots")
    public ApiResult<EdgeNodeView> bindLots(@PathVariable Long nodeId,
                                            @RequestBody UpdateEdgeNodeLotsRequest request) {
        return ApiResult.ok(nodeService.bindLots(nodeId, request));
    }

    @DeleteMapping("/{nodeId}")
    public ApiResult<Void> delete(@PathVariable Long nodeId) {
        nodeService.deleteNode(nodeId);
        return ApiResult.ok();
    }
}
