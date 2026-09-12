package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateParkingOrderRequest;
import com.freepark.cloud.simple.parking.dto.ParkingOrderRefundView;
import com.freepark.cloud.simple.parking.dto.ParkingOrderView;
import com.freepark.cloud.simple.parking.dto.RefundParkingOrderRequest;
import com.freepark.cloud.simple.parking.entity.ParkingOrderStatus;
import com.freepark.cloud.simple.parking.service.ParkingOrderRefundService;
import com.freepark.cloud.simple.parking.service.ParkingOrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 停车订单管理（停车管理 - 停车订单）：记录每次收费请求为停车流水生成的订单与支付金额。
 */
@RestController
@RequestMapping("/api/parking-orders")
public class ParkingOrderController {

    private final ParkingOrderService parkingOrderService;
    private final ParkingOrderRefundService parkingOrderRefundService;

    public ParkingOrderController(ParkingOrderService parkingOrderService,
                                  ParkingOrderRefundService parkingOrderRefundService) {
        this.parkingOrderService = parkingOrderService;
        this.parkingOrderRefundService = parkingOrderRefundService;
    }

    @GetMapping
    public ApiResult<PageResult<ParkingOrderView>> list(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ParkingOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(parkingOrderService.listOrders(
                sessionId, lotId, keyword, status, startDate, endDate, page, size));
    }

    /**
     * 创建停车订单：为指定流水按「当前应收 − 累计已支付 − 待付订单」生成一笔待支付订单
     * （当前已无需再付时返回业务错误，避免重复缴费）。
     */
    @PostMapping
    public ApiResult<ParkingOrderView> create(@RequestBody CreateParkingOrderRequest request) {
        return ApiResult.ok(parkingOrderService.createOrder(
                request == null ? null : request.sessionId()));
    }

    /** 登记收款：订单待支付 → 已支付，并把金额入账到关联流水累计已支付。 */
    @PostMapping("/{orderId}/pay")
    public ApiResult<ParkingOrderView> pay(@PathVariable Long orderId) {
        return ApiResult.ok(parkingOrderService.registerPayment(orderId));
    }

    /** 取消订单：仅待支付订单可取消，释放金额占用。 */
    @PostMapping("/{orderId}/cancel")
    public ApiResult<ParkingOrderView> cancel(@PathVariable Long orderId) {
        return ApiResult.ok(parkingOrderService.cancelOrder(orderId));
    }

    /**
     * 退款：已支付 / 部分退款订单可全部退款或部分退款；缺省金额表示按剩余可退全额退款。
     * 退款金额从关联流水累计已支付中回冲。
     */
    @PostMapping("/{orderId}/refund")
    public ApiResult<ParkingOrderView> refund(
            @PathVariable Long orderId,
            @RequestBody(required = false) RefundParkingOrderRequest request) {
        return ApiResult.ok(parkingOrderService.refundOrder(orderId, request));
    }

    /** 指定停车订单的退款记录（按退款时间倒序）。 */
    @GetMapping("/{orderId}/refunds")
    public ApiResult<List<ParkingOrderRefundView>> refunds(@PathVariable Long orderId) {
        return ApiResult.ok(parkingOrderRefundService.listByOrder(orderId));
    }

    /**
     * 本单对应的缴费流水：线上一次缴清多条流水时返回同一缴款单下的全部停车订单，
     * 管理端人工下单仅返回本单。
     */
    @GetMapping("/{orderId}/payment-sessions")
    public ApiResult<List<ParkingOrderView>> paymentSessions(@PathVariable Long orderId) {
        return ApiResult.ok(parkingOrderService.listPaymentSessions(orderId));
    }
}
