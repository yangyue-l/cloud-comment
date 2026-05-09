package com.yangyue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yangyue.dto.Result;
import com.yangyue.entity.VoucherOrder;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);
}
