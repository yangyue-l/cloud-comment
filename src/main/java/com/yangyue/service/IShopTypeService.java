package com.yangyue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yangyue.dto.Result;
import com.yangyue.entity.ShopType;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryTypeList();

}
