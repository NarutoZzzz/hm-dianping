package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class ApplicationTests {
		
		@Resource
		private CacheClient cacheClient;
		
//		@Resource
//		private ShopServiceImpl shopService;
		
		@Resource
		private RedisIdWorker redisIdWorker;
		
		@Resource
		private StringRedisTemplate stringRedisTemplate;
		
		private ExecutorService es = Executors.newFixedThreadPool(500);
		
		@Test
		public void testIDWorkers() throws InterruptedException {
				
				CountDownLatch latch = new CountDownLatch(300);
				
				Runnable task = () -> {
						for (int i = 0; i < 100; i++) {
								long id = redisIdWorker.nextId("order");
								System.out.println("id = " + id);
						}
						latch.countDown();
				};
				long begin = System.currentTimeMillis();
				for (int i = 0; i < 300; i++) {
						es.submit(task);
						
				}
				latch.await();
				long end = System.currentTimeMillis();
				System.out.println("时间 = " + (end - begin));
		}
		
//		@Test
//		public void TestSaveRedis() throws InterruptedException {
//				Shop shop = shopService.getById(1L);
//				cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
//
//		}
		
		
}
