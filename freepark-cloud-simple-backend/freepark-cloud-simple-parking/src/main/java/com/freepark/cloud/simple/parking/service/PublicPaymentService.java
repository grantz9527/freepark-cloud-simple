package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.pay.PublicOriginResolver;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.parking.dto.AlipayWapPayView;
import com.freepark.cloud.simple.parking.dto.CreatePaymentRequest;
import com.freepark.cloud.simple.parking.dto.PayableQuoteView;
import com.freepark.cloud.simple.parking.dto.PaymentItemView;
import com.freepark.cloud.simple.parking.dto.PaymentOrderView;
import com.freepark.cloud.simple.parking.dto.WeChatJsapiPayView;
import com.freepark.cloud.simple.parking.entity.ParkingOrder;
import com.freepark.cloud.simple.parking.entity.PaymentMethod;
import com.freepark.cloud.simple.parking.entity.PaymentOrder;
import com.freepark.cloud.simple.parking.entity.PaymentOrderStatus;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.event.PaymentSettledEvent;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRepository;
import com.freepark.cloud.simple.parking.repository.PaymentOrderRepository;
import com.freepark.cloud.simple.parking.service.alipay.AlipayWapPayClient;
import com.freepark.cloud.simple.parking.service.wechat.WeChatJsapiPayClient;
import com.freepark.cloud.simple.settings.dto.AlipayPayRuntimeConfig;
import com.freepark.cloud.simple.settings.dto.WeChatJsapiRuntimeConfig;
import com.freepark.cloud.simple.settings.service.AlipayConfigService;
import com.freepark.cloud.simple.settings.service.SystemSettingsService;
import com.freepark.cloud.simple.settings.service.WeChatConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * C 端（用户端网页）在线缴款服务。
 * 系统配置「强制全部支付」时按车牌缴清全部未结流水；允许勾选时只缴请求中的停车记录。
 * <p>
 * 下单时把该车牌的未结流水按流水拆成多笔停车订单（{@code paymentNo} 指向本缴款单），
 * 缴款成功后再逐笔入账到对应流水的累计已缴，实现「一次缴款 = 多条流水的多笔订单」。
 * <p>
 * 入账来源：本地联调确认（{@code freepark.payment.mock-enabled}）、微信 / 支付宝异步通知。
 * 微信或支付宝凭据齐全时走真实渠道；否则在 mock 开启时走联调确认。
 */
@Service
public class PublicPaymentService {

    private static final DateTimeFormatter PAY_NO_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final PaymentOrderRepository payments;
    private final ParkingOrderRepository orders;
    private final ParkingOrderService orderService;
    private final ParkingSessionService sessionService;
    private final SystemSettingsService settingsService;
    private final PayRecordService payRecordService;
    private final PublicOriginResolver publicOrigin;
    private final WeChatConfigService weChatConfig;
    private final WeChatJsapiPayClient weChatJsapi;
    private final AlipayConfigService alipayConfig;
    private final AlipayWapPayClient alipayWap;
    private final ApplicationEventPublisher events;
    private final Environment environment;
    private final EdgeLaneWaitService laneWait;

    /** 是否允许本地联调确认：渠道未接入或凭据不全时可用。 */
    @Value("${freepark.payment.mock-enabled:true}")
    private boolean mockEnabled;

    /**
     * 微信支付附加数据 / 商品描述前缀。dev、test 默认 test，生产留空。
     * 商户平台交易流水的「商品名称」「商户数据包」会带上该标记。
     */
    @Value("${freepark.payment.wechat-attach:}")
    private String wechatAttach;

    public PublicPaymentService(PaymentOrderRepository payments,
                                ParkingOrderRepository orders,
                                ParkingOrderService orderService,
                                ParkingSessionService sessionService,
                                SystemSettingsService settingsService,
                                PayRecordService payRecordService,
                                PublicOriginResolver publicOrigin,
                                WeChatConfigService weChatConfig,
                                WeChatJsapiPayClient weChatJsapi,
                                AlipayConfigService alipayConfig,
                                AlipayWapPayClient alipayWap,
                                ApplicationEventPublisher events,
                                Environment environment,
                                EdgeLaneWaitService laneWait) {
        this.payments = payments;
        this.orders = orders;
        this.orderService = orderService;
        this.sessionService = sessionService;
        this.settingsService = settingsService;
        this.payRecordService = payRecordService;
        this.publicOrigin = publicOrigin;
        this.weChatConfig = weChatConfig;
        this.weChatJsapi = weChatJsapi;
        this.alipayConfig = alipayConfig;
        this.alipayWap = alipayWap;
        this.events = events;
        this.environment = environment;
        this.laneWait = laneWait;
    }

    /**
     * 按车牌下单。强制全部支付时缴清当前全部未结流水；允许勾选时只缴 {@code sessionIds}。
     * 下单前先关闭该车牌尚未完成的缴款单（释放其占用的订单），避免重复占用与重复缴款。
     */
    @Transactional
    public PaymentOrderView createPayment(CreatePaymentRequest request, HttpServletRequest http) {
        String plate = normalizePlate(request == null ? null : request.plateNumber());
        PlateColor color = resolveColor(request == null ? null : request.plateColor());
        PaymentMethod method = resolveMethod(request == null ? null : request.method());
        if (!settingsService.getAllowedPaymentMethods().contains(method.name())) {
            throw new BizException(400, MessageKeys.PAYMENT_METHOD_NOT_ALLOWED);
        }

        WeChatJsapiRuntimeConfig wechatRuntime = weChatConfig.loadJsapiRuntime();
        AlipayPayRuntimeConfig alipayRuntime = alipayConfig.loadRuntime();
        String wxCode = request == null ? null : request.wxCode();

        // 支付宝不走 mock：凭据不全直接失败。微信凭据不全时可在 mock 开启时联调。
        boolean wantRealAlipay = false;
        boolean wantRealWechat = false;
        boolean useMock = false;
        if (method == PaymentMethod.ALIPAY_PAY) {
            if (!alipayRuntime.readyToPay()) {
                throw new BizException(400, MessageKeys.PAYMENT_ALIPAY_NOT_CONFIGURED);
            }
            wantRealAlipay = true;
        } else if (method == PaymentMethod.WECHAT_PAY) {
            wantRealWechat = wechatRuntime.ready();
            useMock = !wantRealWechat;
            if (wantRealWechat && !StringUtils.hasText(wxCode)) {
                throw new BizException(400, MessageKeys.PAYMENT_WECHAT_OAUTH_REQUIRED);
            }
            if (useMock && !mockEnabled) {
                throw new BizException(400, MessageKeys.PAYMENT_WECHAT_NOT_CONFIGURED);
            }
        } else {
            throw new BizException(400, MessageKeys.PAYMENT_METHOD_NOT_ALLOWED);
        }

        WeChatJsapiPayView wxPay = null;
        AlipayWapPayView aliPay = null;
        String openid = null;
        if (wantRealWechat) {
            openid = weChatJsapi.exchangeOpenId(wechatRuntime, wxCode);
        }

        closePendingPayments(plate);
        List<PayableQuoteView> quotes = resolvePayableQuotes(
                plate, color, request == null ? null : request.sessionIds());
        BigDecimal total = BigDecimal.ZERO;
        for (PayableQuoteView quote : quotes) {
            total = total.add(quote.payableYuan());
        }
        if (total.signum() <= 0) {
            throw new BizException(400, MessageKeys.PARKING_ORDER_NOT_PAYABLE);
        }
        PaymentOrder payment = new PaymentOrder();
        payment.setPayNo(nextPayNo());
        payment.setPlateNumber(plate);
        payment.setPlateColor(color);
        payment.setAmountYuan(total);
        payment.setMethod(method);
        payment.setStatus(PaymentOrderStatus.PENDING);
        payment.setMock(useMock);
        payments.save(payment);

        List<ParkingOrder> created = new ArrayList<>(quotes.size());
        List<PaymentItemView> items = new ArrayList<>(quotes.size());
        for (PayableQuoteView quote : quotes) {
            ParkingOrder order = orderService.createPendingOrder(quote.sessionId(), quote, payment.getPayNo());
            created.add(order);
            items.add(PaymentItemView.from(order));
        }
        payRecordService.recordOnlinePay(payment, created);

        if (wantRealWechat) {
            String notifyUrl = weChatConfig.effectiveNotifyUrl(publicOrigin.wechatNotifyUrl(http));
            if (!StringUtils.hasText(notifyUrl)) {
                throw new BizException(400, MessageKeys.PAYMENT_WECHAT_NOT_CONFIGURED);
            }
            String attach = resolveWechatAttach();
            String description = StringUtils.hasText(attach)
                    ? attach + " 停车费 " + plate
                    : "停车费 " + plate;
            wxPay = weChatJsapi.prepay(
                    wechatRuntime, payment.getPayNo(), description, attach, total, openid, notifyUrl);
        }
        if (wantRealAlipay) {
            String notifyUrl = alipayConfig.effectiveNotifyUrl(publicOrigin.alipayNotifyUrl(http));
            if (!StringUtils.hasText(notifyUrl)) {
                throw new BizException(400, MessageKeys.PAYMENT_ALIPAY_NOT_CONFIGURED);
            }
            String returnUrl = publicOrigin.userPayReturnUrl(http, payment.getPayNo());
            if (!StringUtils.hasText(returnUrl)) {
                throw new BizException(400, MessageKeys.PAYMENT_ALIPAY_NOT_CONFIGURED);
            }
            String subject = "停车费 " + plate;
            aliPay = alipayWap.pagePay(
                    alipayRuntime, payment.getPayNo(), subject, total, notifyUrl, returnUrl);
        }

        return toView(payment, items, http, wxPay, aliPay);
    }

    /** 查询缴款单（含按流水拆分的明细），供用户端展示进行中/结果。 */
    @Transactional(readOnly = true)
    public PaymentOrderView getPayment(String payNo, HttpServletRequest http) {
        PaymentOrder payment = requirePayment(payNo);
        return toView(payment, http);
    }

    /**
     * 本地联调确认：模拟渠道回调结果。成功则把拆分订单逐笔入账并把缴款单置为已缴；
     * 失败（用户取消/渠道失败）则释放拆分订单并关闭缴款单。
     */
    @Transactional
    public PaymentOrderView confirmSandbox(String payNo, boolean success, HttpServletRequest http) {
        if (!mockEnabled) {
            throw new BizException(400, MessageKeys.PAYMENT_GATEWAY_UNAVAILABLE);
        }
        PaymentOrder payment = requirePayment(payNo);
        if (!payment.isMock() || payment.getStatus() != PaymentOrderStatus.PENDING) {
            throw new BizException(400, MessageKeys.PAYMENT_BAD_STATE);
        }
        if (success) {
            markPaid(payment, nextTransactionId());
        } else {
            orderService.cancelPaymentOrders(payment.getPayNo());
            payment.setStatus(PaymentOrderStatus.CLOSED);
            payRecordService.markPayClosed(payment.getPayNo());
        }
        payments.save(payment);
        return toView(payment, http);
    }

    /**
     * 渠道异步通知入账：验签通过后由微信/支付宝回调调用。
     * 已支付视为幂等成功；非待支付状态不再改账（仍视为已受理，避免渠道反复重试）。
     */
    @Transactional
    public ChannelSettleResult applyChannelSuccess(String payNo, String transactionId,
                                                   PaymentMethod method, BigDecimal amountYuan) {
        if (!StringUtils.hasText(payNo) || method == null) {
            return ChannelSettleResult.REJECT;
        }
        PaymentOrder payment = payments.findByPayNo(payNo.trim()).orElse(null);
        if (payment == null) {
            return ChannelSettleResult.REJECT;
        }
        if (payment.getStatus() == PaymentOrderStatus.PAID) {
            return ChannelSettleResult.DUPLICATE;
        }
        if (payment.getStatus() != PaymentOrderStatus.PENDING) {
            return ChannelSettleResult.DUPLICATE;
        }
        if (payment.getMethod() != method) {
            return ChannelSettleResult.REJECT;
        }
        if (amountYuan != null && payment.getAmountYuan().compareTo(amountYuan) != 0) {
            return ChannelSettleResult.REJECT;
        }
        String channelTxn = StringUtils.hasText(transactionId) ? transactionId.trim() : payNo.trim();
        markPaid(payment, channelTxn);
        payments.save(payment);
        return ChannelSettleResult.OK;
    }

    private void markPaid(PaymentOrder payment, String transactionId) {
        LocalDateTime payTime = SiteZoneTimes.nowUtc();
        orderService.settlePaymentOrders(payment.getPayNo(), payTime);
        payment.setStatus(PaymentOrderStatus.PAID);
        payment.setTransactionId(transactionId);
        payment.setPayTime(payTime);
        payRecordService.markPaySuccess(payment.getPayNo(), transactionId, payTime);
        events.publishEvent(new PaymentSettledEvent(
                payment.getPayNo(), payment.getPlateNumber(), payment.getPlateColor()));
        sessionService.closeOpenSessionsAfterPaidExit(
                payment.getPlateNumber(),
                payment.getPlateColor(),
                laneWait.findPayableWaitLanes(payment.getPlateNumber(), payment.getPlateColor()));
    }

    /** 关闭缴款单（用户主动放弃 / 重新发起）：释放其拆分订单。 */
    @Transactional
    public PaymentOrderView closePayment(String payNo, HttpServletRequest http) {
        PaymentOrder payment = requirePayment(payNo);
        if (payment.getStatus() != PaymentOrderStatus.PENDING) {
            throw new BizException(400, MessageKeys.PAYMENT_BAD_STATE);
        }
        orderService.cancelPaymentOrders(payment.getPayNo());
        payment.setStatus(PaymentOrderStatus.CLOSED);
        payments.save(payment);
        payRecordService.markPayClosed(payment.getPayNo());
        return toView(payment, http);
    }

    /**
     * 微信支付商户流水备注：配置优先；未配时 dev/test 自动标 test。
     * 会出现在微信「商品名称」和「商户数据包(attach)」。
     */
    private String resolveWechatAttach() {
        if (StringUtils.hasText(wechatAttach)) {
            return wechatAttach.trim();
        }
        if (environment.matchesProfiles("dev", "test", "development")) {
            return "test";
        }
        return "";
    }

    private List<PayableQuoteView> resolvePayableQuotes(String plate, PlateColor color, List<Long> sessionIds) {
        List<PayableQuoteView> quotes = sessionService.listPayableQuotes(plate, color);
        if (settingsService.isForcePayAll()) {
            return quotes;
        }
        Set<Long> wanted = new LinkedHashSet<>();
        if (sessionIds != null) {
            for (Long id : sessionIds) {
                if (id != null && id > 0) {
                    wanted.add(id);
                }
            }
        }
        if (wanted.isEmpty()) {
            throw new BizException(400, MessageKeys.PAYMENT_SESSIONS_REQUIRED);
        }
        Map<Long, PayableQuoteView> byId = new LinkedHashMap<>();
        for (PayableQuoteView quote : quotes) {
            byId.put(quote.sessionId(), quote);
        }
        List<PayableQuoteView> selected = new ArrayList<>(wanted.size());
        for (Long id : wanted) {
            PayableQuoteView quote = byId.get(id);
            if (quote == null) {
                throw new BizException(400, MessageKeys.PAYMENT_SESSIONS_INVALID);
            }
            selected.add(quote);
        }
        return selected;
    }

    /** 关闭该车牌尚未完成的缴款单，释放其占用的拆分订单（下单前清理，避免重复占用）。 */
    private void closePendingPayments(String plate) {
        for (PaymentOrder pending : payments.findByStatusAndPlateNumber(PaymentOrderStatus.PENDING, plate)) {
            orderService.cancelPaymentOrders(pending.getPayNo());
            pending.setStatus(PaymentOrderStatus.CLOSED);
            payments.save(pending);
            payRecordService.markPayClosed(pending.getPayNo());
        }
    }

    private PaymentOrderView toView(PaymentOrder payment, HttpServletRequest http) {
        return toView(payment, loadItems(payment.getPayNo()), http, null, null);
    }

    private PaymentOrderView toView(PaymentOrder payment, List<PaymentItemView> items,
                                    HttpServletRequest http, WeChatJsapiPayView wxPay) {
        return toView(payment, items, http, wxPay, null);
    }

    private PaymentOrderView toView(PaymentOrder payment, List<PaymentItemView> items,
                                    HttpServletRequest http, WeChatJsapiPayView wxPay,
                                    AlipayWapPayView aliPay) {
        return PaymentOrderView.from(
                payment, items, publicOrigin.userPayReturnUrl(http, payment.getPayNo()), wxPay, aliPay);
    }

    private PaymentOrder requirePayment(String payNo) {
        if (!StringUtils.hasText(payNo)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return payments.findByPayNo(payNo.trim())
                .orElseThrow(() -> new BizException(404, MessageKeys.PAYMENT_NOT_FOUND));
    }

    private List<PaymentItemView> loadItems(String payNo) {
        List<ParkingOrder> list = orders.findByPaymentNoOrderByIdAsc(payNo);
        List<PaymentItemView> items = new ArrayList<>(list.size());
        for (ParkingOrder order : list) {
            items.add(PaymentItemView.from(order));
        }
        return items;
    }

    private static String normalizePlate(String plateNumber) {
        String plate = plateNumber == null ? null : plateNumber.trim().toUpperCase();
        if (!StringUtils.hasText(plate)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return plate;
    }

    private static PlateColor resolveColor(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return PlateColor.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
    }

    private static PaymentMethod resolveMethod(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        try {
            return PaymentMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
    }

    /** 生成唯一缴款单号：UTC 时间戳 + 随机数，对外展示/对账用。 */
    private static String nextPayNo() {
        String stamp = SiteZoneTimes.nowUtc().format(PAY_NO_STAMP);
        int random = ThreadLocalRandom.current().nextInt(9000) + 1000;
        return "PY" + stamp + random;
    }

    /** 生成联调流水号，便于与真实渠道流水号区分。 */
    private static String nextTransactionId() {
        String stamp = SiteZoneTimes.nowUtc().format(PAY_NO_STAMP);
        int random = ThreadLocalRandom.current().nextInt(9000) + 1000;
        return "SB" + stamp + random;
    }
}
