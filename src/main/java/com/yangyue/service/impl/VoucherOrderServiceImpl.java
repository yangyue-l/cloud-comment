package com.yangyue.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangyue.dto.Result;
import com.yangyue.entity.SeckillVoucher;
import com.yangyue.entity.VoucherOrder;
import com.yangyue.mapper.VoucherOrderMapper;
import com.yangyue.service.ISeckillVoucherService;
import com.yangyue.service.IVoucherOrderService;
import com.yangyue.utils.RedisIdWorker;
import com.yangyue.utils.SimpleRedisLock;
import com.yangyue.utils.UserHolder;

import java.time.LocalDateTime;

import javax.annotation.Resource;

import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始");
        }
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已经结束");
        }
        //判断库存
        if(voucher.getStock() < 1){
            return Result.fail("库存不足");
        }

        Long userId = UserHolder.getUser().getId();
        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
        boolean isLock = lock.tryLock(1200);

        if(!isLock){
            return Result.fail("不允许重复下单");
        }

        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();

        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if(count > 0){
            return Result.fail("用户已经购买过一次");
        }


        boolean success =  seckillVoucherService.update()
                        .setSql("stock = stock - 1")
                        .eq("voucher_id", voucherId)
                        .gt("stock", 0)
                        .update();

        if(!success){
            return Result.fail("库存不足");
        }



        VoucherOrder voucherOrder = new VoucherOrder();
        //获取并设置订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        
        voucherOrder.setUserId(userId);

        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        return Result.ok(orderId);
    }

}
