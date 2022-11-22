package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author SpiderMan_Biu
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
		@Resource
		private StringRedisTemplate stringRedisTemplate;
		
		@Override
		public Result queryList() {
				String key = "shop:list";
				// 1.从Redis中查询商铺缓存
				List<String> shoptypeList = stringRedisTemplate.opsForList().range(key, 0, -1);
				// 2. 判断是否存在
				if (!CollectionUtil.isEmpty(shoptypeList)) {
						//3.存在数据
						List<ShopType> typeList = JSONUtil.toList(shoptypeList.get(0), ShopType.class);
						//返回数据
						return Result.ok(typeList);
				}
				
				// 4. 不存在 根据Id查询数据库
				List<ShopType> typeList = query().orderByAsc("sort").list();
				// 5. 数据库不存在 返回错误信息
				if (CollectionUtil.isEmpty(typeList)) {
						return Result.fail("数据列表没有展示出来");
				}
				// 6. 数据库存在,先把数据写入到Redis中
				String toJsonStr = JSONUtil.toJsonStr(typeList);
				stringRedisTemplate.opsForList().leftPushAll(key, toJsonStr);
				// 7. 返回
				return Result.ok(typeList);
		}
}
