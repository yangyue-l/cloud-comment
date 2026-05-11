package com.yangyue.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangyue.dto.Result;
import com.yangyue.entity.Shop;
import com.yangyue.mapper.ShopMapper;
import com.yangyue.service.IShopService;
import com.yangyue.utils.CacheClient;
import com.yangyue.utils.RedisConstants;
import com.yangyue.utils.SystemConstants;

import cn.hutool.core.util.StrUtil;

import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
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


    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //判断是否需要根据坐标查询
        if(x == null || y == null){
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        //计算分也参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        GeoResults<GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                                    .search(key,
                                         GeoReference.fromCoordinate(x, y),
                                         new Distance(5000),
                                         RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));

        if(results == null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<GeoLocation<String>>> list = results.getContent();

        if(list.size() <= from){
            return Result.ok(Collections.EMPTY_LIST);
        }

        List<Long> ids = new ArrayList<>(list.size());
        HashMap<String,Distance> distanceMap = new HashMap<>(list.size());

        list.stream().skip(from).forEach(result -> {
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });

        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

        for(Shop shop : shops){
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }


        return Result.ok(shops);
    }

}