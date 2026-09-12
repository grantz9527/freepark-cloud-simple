package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.parking.dto.CreatePaymentRequest;
import com.freepark.cloud.simple.parking.dto.PaymentOrderView;
import com.freepark.cloud.simple.parking.service.PublicPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端（用户端网页）公开缴款接口：面向车主，无需登录。
 * 与查费接口同属 /api/public 命名空间（WebAuthConfig 整体放行），仅暴露缴款相关能力，
 * 不含任何车场/流水管理能力。
 */
@RestController
@RequestMapping("/api/public/payment")
public class PublicPaymentController {

    private final PublicPaymentService paymentService;

    public PublicPaymentController(PublicPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 按车牌下单：把该车牌当前全部未结流水一次缴清，返回缴款单（含按流水拆分的明细）。 */
    @PostMapping
    public PaymentOrderView create(@RequestBody(required = false) CreatePaymentRequest request,
                                   HttpServletRequest http) {
        return paymentService.createPayment(request, http);
    }

    /** 查询缴款单（含拆分明细），供用户端展示进行中/结果。 */
    @GetMapping("/{payNo}")
    public PaymentOrderView detail(@PathVariable("payNo") String payNo, HttpServletRequest http) {
        return paymentService.getPayment(payNo, http);
    }

    /**
     * 本地联调确认（模拟渠道回调结果）：{@code success=false} 表示用户取消或渠道失败。
     * 仅在 {@code freepark.payment.mock-enabled=true} 时可用。
     */
    @PostMapping("/{payNo}/sandbox-confirm")
    public PaymentOrderView confirm(@PathVariable("payNo") String payNo,
                                    @RequestParam(value = "success", defaultValue = "true") boolean success,
                                    HttpServletRequest http) {
        return paymentService.confirmSandbox(payNo, success, http);
    }

    /** 关闭缴款单（用户主动放弃 / 重新发起）：释放其占用的订单。 */
    @PostMapping("/{payNo}/close")
    public PaymentOrderView close(@PathVariable("payNo") String payNo, HttpServletRequest http) {
        return paymentService.closePayment(payNo, http);
    }
}
