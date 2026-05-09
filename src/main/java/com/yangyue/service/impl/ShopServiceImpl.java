package com.yangyue.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangyue.dto.Result;
import com.yangyue.entity.Shop;
import com.yangyue.mapper.ShopMapper;
import com.yangyue.service.IShopService;
import com.yangyue.utils.CacheClient;
import com.yangyue.utils.RedisConstants;


import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;


    /**
     * 根据Id查询商户信息
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        //解决缓存穿透
        // Shop shop = cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, 
        //     Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);


        
        //逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, 
            Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop == null) {
            return Result.fail("店铺不存在");
        }

        // String key = RedisConstants.CACHE_SHOP_KEY + id;
        // String shopJson = stringRedisTemplate.opsForValue().get(key);
        // if(StrUtil.isNotBlank(shopJson)){
        //     Shop shop = JSONUtil.toBean(shopJson, Shop.class);
        //     return Result.ok(shop);
        // }
        // if(shopJson != null){
        //     return Result.fail("店铺信息不存在");
        // }


        // Shop shop = getById(id);

        // if(shop == null){
        //     //将空值写入
        //     stringRedisTemplate.opsForValue().set(key, "",
        //                                 RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
        
        //     return Result.fail("店铺不存在");
        // }

        // stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),
        //                                 RedisConstants.CACHE_SHOP_TTL,TimeUnit.MINUTES);

        return Result.ok(shop);
    }


    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }

}
