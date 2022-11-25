package com.hmdp.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author Ironman
 * @implSpec
 * @doto 2022/11/23 12:19
 */
@Component
@Data
public class RedisIdWorker {
		//开始的时间戳
		private static final long BEGIN_TIMESTAMP = 1640995200L;
		//序列号的位数
		private static final long COUNT_BITS = 32;
		
		private StringRedisTemplate stringRedisTemplate;

		public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
				this.stringRedisTemplate = stringRedisTemplate;
		}
		
		public long nextId(String keyprefix) {
				//1.生成时间戳
				LocalDateTime now = LocalDateTime.now();
				long nowsend = now.toEpochSecond(ZoneOffset.UTC);
				long timestamp = nowsend - BEGIN_TIMESTAMP;
				//2.生成序列号
				//2.1获取当前日期
				String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
				//2.2自增长
				Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyprefix + ":" + "20220910");
				//3.拼接并返回
				
				return timestamp << COUNT_BITS | count;
		}
		
		public static void main(String[] args) {
				LocalDateTime time = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
				long second = time.toEpochSecond(ZoneOffset.UTC);
				System.out.println("second" + second);
		}
		
}
