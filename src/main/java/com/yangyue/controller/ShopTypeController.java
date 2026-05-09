package com.yangyue.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yangyue.dto.Result;
import com.yangyue.service.IShopTypeService;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    // @GetMapping("list")
    // public Result queryTypeList() {
    //     List<ShopType> typeList = typeService
    //             .query().orderByAsc("sort").list();
    //     return Result.ok(typeList);

    // }
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryTypeList();
    }
}
