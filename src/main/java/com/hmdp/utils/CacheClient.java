package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * @author Ironman
 * @doto 2022/11/22 20:03
 */
@Slf4j
@Component
public class CacheClient {
		@Resource
		private final StringRedisTemplate stringRedisTemplate;
		
		
		public CacheClient(StringRedisTemplate stringRedisTemplate) {
				this.stringRedisTemplate = stringRedisTemplate;
		}
		
		
		//定义方法
		public void set(String key, Object value, Long time, TimeUnit unit) {
				stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
		}
		
		// TODO: 2022/11/22 逻辑过期
		public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
				//设置逻辑过期
				RedisData redisData = new RedisData();
				redisData.setData(value);
				redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
				
				//写入redis
				stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
		}
		
		// TODO: 2022/11/22 get方法
		public <R, ID> R queryWithPassThrough(
						String keyprefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
				String key = keyprefix + id;
				// 1.从Redis中查询商铺缓存
				String Json = stringRedisTemplate.opsForValue().get(key);
				// 2. 判断是否存在
				if (StrUtil.isNotBlank(Json)) {
						// 3. 存在,返回数据
						return JSONUtil.toBean(Json, type);
				}
				// TODO: 2022/11/22 判断是否命中的是null值
				if (Json != null) {
						//返回错误信息
						return null;
				}
				// 4. 不存在 根据Id查询数据库
				R r = dbFallback.apply(id);
				
				// 5. 数据库不存在 返回错误信息
				if (r == null) {
						// TODO: 2022/11/22 将null值返回给redis
						stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
						//返回错误信息
						return null;
				}
				// 6. 数据库存在,先把数据写入到Redis中
				// TODO: 2022/11/22 加入查询时间过期策略为30分钟,并设置分钟的时间为常量
				this.set(key, r, time, unit);
				
				// 7. 返回
				return r;
		}
		
		// TODO: 2022/11/22 get方法
		private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
		
		// TODO: 2022/11/22 逻辑过期实现缓存击穿方法
		public <R, ID> R queryWithLogicalExpire(String keyprefix, ID id, Class<R> type, Function<ID, R> dbfallback, Long time, TimeUnit unit) {
				String key = keyprefix + id;
				// 1.从Redis中查询商铺缓存
				String Json = stringRedisTemplate.opsForValue().get(key);
				// 2. 判断是否存在
				if (StrUtil.isBlank(Json)) {
						// 3. 存在,返回数据
						return null;
				}
				//4.1命中.需要先把json反序列化为对象
				RedisData redisData = JSONUtil.toBean(Json, RedisData.class);
				R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
				LocalDateTime expireTime = redisData.getExpireTime();
				//5.判断是否过期
				if (expireTime.isAfter(LocalDateTime.now())) {
						//5.1未过期,返回店铺信息
						return r;
				}
				//5.2已过期,需要缓存重建
				
				//6.1.获取互斥锁
				String LocakKey = LOCK_SHOP_KEY + id;
				boolean islock = trylock(LocakKey);
				//6.2判断是否获取锁成功F
				if (islock) {
						// TODO: 2022/11/22 6.3成功.开启独立线程实现缓存重建
						CACHE_REBUILD_EXECUTOR.submit(() -> {
								try {
										//查询数据库
										R rl = dbfallback.apply(id);
										//写入Redis
										this.setWithLogicalExpire(key, rl, time, unit);
										
								} catch (Exception e) {
										throw new RuntimeException(e);
								} finally {
										//释放锁
										
										unlock(LocakKey);
										
								}
						});
						
				}
				//6.4失败,返回过期的商铺数据
				return r;
		}
		
		private boolean trylock(String key) {
				Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
				return BooleanUtil.isTrue(flag);
		}
		
		private void unlock(String key) {
				stringRedisTemplate.delete(key);
		}
		
}

