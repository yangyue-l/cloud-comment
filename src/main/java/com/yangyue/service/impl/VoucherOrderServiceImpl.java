package com.yangyue.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangyue.dto.Result;
import com.yangyue.entity.VoucherOrder;
import com.yangyue.mapper.VoucherOrderMapper;
import com.yangyue.service.ISeckillVoucherService;
import com.yangyue.service.IVoucherOrderService;
import com.yangyue.utils.RedisIdWorker;
import com.yangyue.utils.UserHolder;

import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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

    @Resource 
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static{
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    //创建阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{

        @Override
        public void run() {
            while (true) {
                try {
                    //获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    //创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    log.error("处理订单异常",e);
                }
            }

        }

    }

    private IVoucherOrderService proxy;


    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(
                    SECKILL_SCRIPT, 
                    Collections.emptyList(),
                    voucherId.toString(),
                    userId.toString()
                );
        int r = result.intValue();
        if(r != 0){
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }

        long orderId = redisIdWorker.nextId("order");

        VoucherOrder voucherOrder = new VoucherOrder();
        //获取并设置订单id
        voucherOrder.setId(orderId);
        
        voucherOrder.setUserId(userId);

        voucherOrder.setVoucherId(voucherId);
        //放入阻塞队列
        orderTasks.add(voucherOrder);
        
        proxy = (IVoucherOrderService) AopContext.currentProxy();


        return Result.ok(orderId);
    }
    // @Override
    // public Result seckillVoucher(Long voucherId) {
    //     //查询优惠券
    //     SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
    //     if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
    //         return Result.fail("秒杀尚未开始");
    //     }
    //     if(voucher.getEndTime().isBefore(LocalDateTime.now())){
    //         return Result.fail("秒杀已经结束");
    //     }
    //     //判断库存
    //     if(voucher.getStock() < 1){
    //         return Result.fail("库存不足");
    //     }

    //     Long userId = UserHolder.getUser().getId();
    //     //创建锁对象
    //     // SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
    //     RLock lock = redissonClient.getLock("order:" + userId);

    //     boolean isLock = lock.tryLock();

    //     if(!isLock){
    //         return Result.fail("不允许重复下单");
    //     }

    //     try {
    //         IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
    //         return proxy.createVoucherOrder(voucherId);
    //     } finally {
    //         lock.unlock();
    //     }
    // }

    public void handleVoucherOrder(VoucherOrder voucherOrder) {
        //获取用户Id
        Long userId = voucherOrder.getUserId();

        //创建锁对象
        RLock lock = redissonClient.getLock("order:" + userId);

        boolean isLock = lock.tryLock();

        if(!isLock){
            log.error("不允许重复下单");
            return;
        }

        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }


    }

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();

        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if(count > 0){
            log.error("用户已经购买过一次");
            return;
        }


        boolean success =  seckillVoucherService.update()
                        .setSql("stock = stock - 1")
                        .eq("voucher_id", voucherOrder.getVoucherId())
                        .gt("stock", 0)
                        .update();

        if(!success){
            log.error("库存不足！");
            return;
        }


        save(voucherOrder);

        
    }

}
