package com.yangyue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yangyue.dto.Result;
import com.yangyue.entity.Voucher;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
