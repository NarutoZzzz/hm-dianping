package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author SpiderMan_Biu
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
		
		@Resource
		private StringRedisTemplate stringRedisTemplate;
		
		@Resource
		private CacheClient cacheClient;
		
		
		// TODO: 2022/11/18 根据Id查询商铺信息
		@Override
		public Result queryById(Long id) {
				//缓存穿透
				Shop shop = queryWithPassThrough(id);
				
				cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
				
				//使用互斥锁解决缓存击穿
//				Shop shop = queryWithMutex(id);
				
				//逻辑过期策略实践
//				Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);
				if (shop == null) {
						return Result.fail("店铺不存在!");
				}
				// 7. 返回
				return Result.ok(shop);
		}
		
		// TODO: 2022/11/22 线程池
		private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
		
		// TODO: 2022/11/22 逻辑过期实现缓存击穿方法
//		public Shop queryWithLogicalExpire(Long id) {
//				String key = CACHE_SHOP_KEY + id;
//				// 1.从Redis中查询商铺缓存
//				String shopJson = stringRedisTemplate.opsForValue().get(key);
//				// 2. 判断是否存在
//				if (StrUtil.isBlank(shopJson)) {
//						// 3. 存在,返回数据
//						return null;
//				}
//				//4.1命中.需要先把json反序列化为对象
//				RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//				Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//				LocalDateTime expireTime = redisData.getExpireTime();
//				//5.判断是否过期
//				if (expireTime.isAfter(LocalDateTime.now())) {
//						//5.1未过期,返回店铺信息
//						return shop;
//				}
//				//5.2已过期,需要缓存重建
//
//				//6.1.获取互斥锁
//				String LocakKey = LOCK_SHOP_KEY + id;
//				boolean islock = trylock(LocakKey);
//				//6.2判断是否获取锁成功F
//				if (islock) {
//						// TODO: 2022/11/22 6.3成功.开启独立线程实现缓存重建
//						CACHE_REBUILD_EXECUTOR.submit(() -> {
//								try {
//										//重建缓存
//										this.saveShop2Redis(id, 20L);
//								} catch (Exception e) {
//										throw new RuntimeException(e);
//								} finally {
//										//释放锁
//
//										unlock(LocakKey);
//
//								}
//						});
//
//				}
//				//6.4失败,返回过期的商铺数据
//				return shop;
//		}
		
		public Shop queryWithPassThrough(Long id) {
				String key = CACHE_SHOP_KEY + id;
				// 1.从Redis中查询商铺缓存
				String shopJson = stringRedisTemplate.opsForValue().get(key);
				// 2. 判断是否存在
				if (StrUtil.isNotBlank(shopJson)) {
						// 3. 存在,返回数据
						Shop shop = JSONUtil.toBean(shopJson, Shop.class);
						return shop;
				}
				// TODO: 2022/11/22 判断是否命中的是null值
				if (shopJson != null) {
						//返回错误信息
						return null;
				}
				// 4. 不存在 根据Id查询数据库
				Shop shop = getById(id);
				
				// 5. 数据库不存在 返回错误信息
				if (shop == null) {
						// TODO: 2022/11/22 将null值返回给redis
						stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
						//返回错误信息
						return null;
				}
				// 6. 数据库存在,先把数据写入到Redis中
				// TODO: 2022/11/22 加入查询时间过期策略为30分钟,并设置分钟的时间为常量
				stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
				// 7. 返回
				return shop;
		}
		
		//互斥锁
//		public Shop queryWithMutex(Long id) {
//				String key = CACHE_SHOP_KEY + id;
//				// 1.从Redis中查询商铺缓存
//				String shopJson = stringRedisTemplate.opsForValue().get(key);
//				// 2. 判断是否存在
//				if (StrUtil.isNotBlank(shopJson)) {
//						// 3. 存在,返回数据
//						Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//						return shop;
//				}
//				// TODO: 2022/11/22 判断是否命中的是null值
//				if (shopJson != null) {
//						//返回错误信息
//						return null;
//				}
//				// TODO: 2022/11/22 开始实现缓存重建
//				//4.1获取互斥锁
//				String lockKey = "lock:shop:" + id;
//				Shop shop = null;
//				try {
//						boolean islock = trylock(lockKey);
//						//4.2判断是否获取成功
//						if (!islock) {
//								//4.3如果失败,则休眠并重试
//								Thread.sleep(50);
//								return queryWithMutex(id);//递归
//						}
//						//4.4如果成功根据id查询数据库
//						shop = getById(id);
//						// TODO: 2022/11/22 模拟查询数据库延迟
//						Thread.sleep(200);
//						// 5. 数据库不存在 返回错误信息
//						if (shop == null) {
//								// TODO: 2022/11/22 将null值返回给redis
//								stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//								//返回错误信息
//								return null;
//						}
//						// 6. 数据库存在,先把数据写入到Redis中
//						// TODO: 2022/11/22 加入查询时间过期策略为30分钟,并设置分钟的时间为常量
//						stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//				} catch (InterruptedException e) {
//						throw new RuntimeException(e);
//				} finally {
//						//7.释放互斥锁
//						unlock(lockKey);
//				}
//				//8. 返回
//				return shop;
//		}
		
		@Override
		@Transactional
		// TODO: 2022/11/22 更新了缓存策略:先查询数据库再去删除Redis缓存中的数据
		public Result update(Shop shop) {
				Long id = shop.getId();
				if (id == null) {
						return Result.fail("店铺Id不能为空");
				}
				//1.更新数据库
				updateById(shop);
				//2.删除缓存
				stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
				return Result.ok();
		}
		
		// TODO: 2022/11/22 获取锁
//		private boolean trylock(String key) {
//				Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//				return BooleanUtil.isTrue(flag);
//		}
//
//		// TODO: 2022/11/22 释放锁
//		private void unlock(String key) {
//				stringRedisTemplate.delete(key);
//		}
//
//
//		// TODO: 2022/11/22 把shop添加到redis当中的方法
//		public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
//				//1.查询店铺数据
//				Shop shop = getById(id);
//				Thread.sleep(200);
//				//2.封装逻辑过期时间
//				RedisData redisData = new RedisData();
//				redisData.setData(shop);
//				redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));//expireSeconds)过期的秒数
//				//3.写入到redis中
//				stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//		}
}
