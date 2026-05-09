package com.yangyue.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangyue.dto.Result;
import com.yangyue.entity.ShopType;
import com.yangyue.mapper.ShopTypeMapper;
import com.yangyue.service.IShopTypeService;
import com.yangyue.utils.RedisConstants;

import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        List<String> shopTypeList = stringRedisTemplate.opsForList().range(key, 0, -1);

        if(shopTypeList != null && !shopTypeList.isEmpty()){
            List<ShopType> shopTypes = shopTypeList.stream()
                                        .map(json -> JSONUtil.toBean(json, ShopType.class))
                                        .collect(Collectors.toList());
            return Result.ok(shopTypes);
        }

        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        List<String> jsonList = shopTypes.stream().map(JSONUtil::toJsonStr)
                                    .collect(Collectors.toList());
        
        stringRedisTemplate.opsForList().rightPushAll(key, jsonList);

        return Result.ok(shopTypes);
    }


}
