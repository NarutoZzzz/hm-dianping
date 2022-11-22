package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
@RunWith(SpringRunner.class)
class ApplicationTests {
		
		@Resource
		private CacheClient cacheClient;
		
		@Resource
		private ShopServiceImpl shopService;
		
		@Test
		public void TestSaveRedis() throws InterruptedException {
				Shop shop = shopService.getById(1L);
				cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
				
		}
		
		
}
