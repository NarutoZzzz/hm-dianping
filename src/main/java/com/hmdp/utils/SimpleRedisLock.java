package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author Ironman
 * @doto 2022/11/24 13:36
 */

public class SimpleRedisLock implements ILock {
		
		private String name;
		
		private StringRedisTemplate stringRedisTemplate;
		
		public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
				this.name = name;
				this.stringRedisTemplate = stringRedisTemplate;
		}
		
		private static final String KEY_PREFIX = "lock:";
		private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
		
		// TODO: 2022/11/25 定义Lua脚本
		private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
		
		static {
				UNLOCK_SCRIPT = new DefaultRedisScript<>();
				//指定脚本
				UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
				UNLOCK_SCRIPT.setResultType(Long.class);
		}
		
		
		@Override
		public boolean trylock(int timeOutSec) {
				
				//获取线程标识
				String threadId = ID_PREFIX + Thread.currentThread().getId();
				
				//获取锁
				Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeOutSec, TimeUnit.SECONDS);
				
				// TODO: 2022/11/24 直接返回success可能会造成引用数据类型与基本数据类型自动拆箱失败,返回空指针异常,故而实现以下方法
				return Boolean.TRUE.equals(success);
		}
		
		// TODO: 2022/11/25 利用Lua脚本编写释放锁的方法 解决误删情况
		@Override
		public void unlock() {
				//1.调用Lua脚本
				stringRedisTemplate.execute(
								UNLOCK_SCRIPT,
								Collections.singletonList(KEY_PREFIX),
								ID_PREFIX + Thread.currentThread().getId()
				);
				
		}
		
		/*
		 * 释放锁
		 * */
//		@Override
//		public void unlock() {
//				//获取线程标识
//				String threadId = Id_prefix + Thread.currentThread().getId();
//				//获取锁中的标识
//				String id = stringRedisTemplate.opsForValue().get(Key_prefix);
//				//判断标识是否一致
//				if (threadId.equals(id)) {
//						//释放锁
//						stringRedisTemplate.delete(Key_prefix + name);
//
//				}
//		}
}
