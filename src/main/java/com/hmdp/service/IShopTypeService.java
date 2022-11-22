package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author SpiderMan_Biu
 * @since 2021-12-22
 */
public interface IShopTypeService extends IService<ShopType> {
		
		Result queryList();
}
