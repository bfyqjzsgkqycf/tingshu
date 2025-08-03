package com.lsj.tingshu.order.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.login.annotation.TingShuLogin;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.model.order.OrderInfo;
import com.lsj.tingshu.order.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsj.tingshu.vo.order.OrderInfoVo;
import com.lsj.tingshu.vo.order.TradeVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;

	// Request URL: http://localhost:8500/api/order/orderInfo/trade
	@PostMapping("/trade")
	@TingShuLogin
	public Result trade(@RequestBody TradeVo tradeVo) {
		OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo);

		List<Long> exitPaidItemIds = orderInfoVo.getExitPaidItemIds();
		if (!CollectionUtils.isEmpty(exitPaidItemIds)) {
			return Result.build(exitPaidItemIds, 320, "该购买项已经支付过!");
		}
		List<Long> exitOrdedItemIds = orderInfoVo.getExitOrdedItemIds();
		if (!CollectionUtils.isEmpty(exitOrdedItemIds)) {
			return Result.fail(exitOrdedItemIds);
		}

		return Result.ok(orderInfoVo);
	}

	// Request URL: http://localhost:8500/api/order/orderInfo/submitOrder
	@PostMapping("/submitOrder")
	@TingShuLogin
	public Result submitOrder(@RequestBody @Validated OrderInfoVo orderInfoVo) {
		Map<String, Object> orderNoMap = orderInfoService.submitOrder(orderInfoVo);
		return Result.ok(orderNoMap);
	}

	// Request URL: http://localhost:8500/api/order/orderInfo/getOrderInfo/TlDUrveibqNGVZAROO
	@GetMapping("/getOrderInfo/{orderNo}")
	@TingShuLogin
	public Result getOrderInfo(@PathVariable(value = "orderNo") String orderNo) {
		OrderInfo orderInfoAndDetail = orderInfoService.getOrderInfoAndDetailByOrderNo(orderNo);
		return Result.ok(orderInfoAndDetail);
	}

	// Request URL: http://localhost:8500/api/order/orderInfo/findUserPage/1/10
	@GetMapping("/findUserPage/{pn}/{pz}")
	@TingShuLogin
	public Result findUserPage(@PathVariable(value = "pn") Long pn,
							   @PathVariable(value = "pz") Long pz) {

		IPage<OrderInfo> page = new Page<>(pn, pz);

		Long userId = AuthContextHolder.getUserId();
		page = orderInfoService.findUserPage(page, userId);
		return Result.ok(page);
	}

}

