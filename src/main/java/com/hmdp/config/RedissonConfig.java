package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ironman
 * @doto 2022/11/25 13:20
 */
@Configuration
public class RedissonConfig {
		
		@Bean
		public RedissonClient redissonClient() {
				//配置
				Config config = new Config();
				config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("root");
				//创建Redisson.create对象
				return Redisson.create(config);
				
		}
		
//		@Bean
//		public RedissonClient redissonClient2() {
//				//配置
//				Config config = new Config();
//				config.useSingleServer().setAddress("redis://127.0.0.1:6380").setPassword("root");
//				//创建Redisson.create对象
//				return Redisson.create(config);
//
//		}
//
//		@Bean
//		public RedissonClient redissonClient3() {
//				//配置
//				Config config = new Config();
//				config.useSingleServer().setAddress("redis://127.0.0.1:6381").setPassword("root");
//				//创建Redisson.create对象
//				return Redisson.create(config);
//
//		}
}
