package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
		
		@Resource
		private StringRedisTemplate stringRedisTemplate;
		
		
		// TODO: 2022/11/18 根据Id查询商铺信息
		@Override
		public Result queryById(Long id) {
				String key = CACHE_SHOP_KEY + id;
				// 1.从Redis中查询商铺缓存
				String shopJson = stringRedisTemplate.opsForValue().get(key);
				// 2. 判断是否存在
				if (StrUtil.isNotBlank(shopJson)) {
						// 3. 存在,返回数据
						Shop shop = JSONUtil.toBean(shopJson, Shop.class);
						return Result.ok(shop);
				}
				// 4. 不存在 根据Id查询数据库
				Shop shop = getById(id);
				// 5. 数据库不存在 返回错误信息
				if (shop == null) {
						return Result.ok("店铺不存在!");
				}
				// 6. 数据库存在,先把数据写入到Redis中
				stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));
				// 7. 返回
				return Result.ok(shop);
		}
}
