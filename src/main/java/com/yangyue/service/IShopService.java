package com.yangyue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yangyue.dto.Result;
import com.yangyue.entity.Shop;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据Id查询商户信息
     * @param id
     * @return
     */
    Result queryById(Long id);

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);

}
