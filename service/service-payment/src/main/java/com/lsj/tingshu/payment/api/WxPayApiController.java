package com.lsj.tingshu.payment.api;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.login.annotation.TingShuLogin;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.model.user.UserInfo;
import com.lsj.tingshu.payment.service.WxPayService;
import com.lsj.tingshu.user.client.UserInfoFeignClient;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment/wxPay")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    /**
     * param1:交易类型 1301：正常下单支付   反之：余额充值
     * param2：订单编号
     * 作用：生成支付二维码【1.下单 选择微信支付 2.充钱】
     *
     * @return
     */
    @PostMapping("/createJsapi/{tradeType}/{orderNo}")
    @TingShuLogin
    public Result createJsapi(@PathVariable(value = "tradeType") String tradeType,
                              @PathVariable(value = "orderNo") String orderNo) {


        Map<String, Object> map = wxPayService.createJsapi(tradeType, orderNo);
        return Result.ok(map);
    }

    // Request URL: http://localhost:8500/api/payment/wxPay/queryPayStatus/GgmBzjGUXQppKCpGOf

    /**
     * 如果说请求返回的true 就代表该订单支付成功 发反之 在30s之内发送10次 轮训问该订单什么时候支付成功
     *
     * @param orderNo
     * @return
     */

    @GetMapping("/queryPayStatus/{orderNo}")
    @TingShuLogin
    public Result queryPayStatus(@PathVariable(value = "orderNo") String orderNo) {

        Transaction transaction = wxPayService.queryPayStatus(orderNo);
        // 1.该笔订单支付成功（订单的支付状态从notpay流转为success）
        if (transaction != null && transaction.getTradeState().equals(Transaction.TradeStateEnum.SUCCESS)) {

            wxPayService.processWxPaidSuccess(orderNo, AuthContextHolder.getUserId());

            return Result.ok(true);
        }
        // 2.该笔订单未支付成功
        return Result.ok(false);
    }


    @PostMapping("/notify")   // 最终一致性
    @SneakyThrows
    public Map<String, Object> asyncNotify(HttpServletRequest httpServletRequest) {
        System.out.println("异步回调进入了....");
        Transaction transaction = wxPayService.asyncNotify(httpServletRequest);
        if (transaction != null && transaction.getTradeState().equals(Transaction.TradeStateEnum.SUCCESS)) {
            // 该用户订单支付成功
            String openid = transaction.getPayer().getOpenid();
            Result<UserInfo> userInfoResult = userInfoFeignClient.getUserInfoByOpenId(openid);
            UserInfo userInfoData = userInfoResult.getData();
            Assert.notNull(userInfoData, "远程调用用户微服务获取用户信息失败");

            wxPayService.processWxPaidSuccess(transaction.getOutTradeNo(), userInfoData.getId());
            // 给微信回调回复成功的应答 确保微信回调不会继续进来
            Map<String, Object> ackMap = new HashMap<>();
            ackMap.put("code", "SUCCESS");
            ackMap.put("message", "成功");
            return ackMap;
        }

        Map<String, Object> noAckMap = new HashMap<>();
        noAckMap.put("code", "FAIL");
        noAckMap.put("message", "失败");
        return noAckMap;
    }

}
