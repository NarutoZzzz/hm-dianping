package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author SpiderMan_Biu
 * @implSpec 拦截器
 * @doto 2022/11/18 13:54
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {
		
		private StringRedisTemplate stringRedisTemplate;
		
		public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
				this.stringRedisTemplate = stringRedisTemplate;
		}
		
		//前置拦截器
		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
				//1.获取session
				// TODO: 2022/11/18 1.获取请求头中的token
				String token = request.getHeader("authorization");
				if (StrUtil.isBlank(token)) {
						return true;
				}
				// TODO: 2022/11/18 2,基于TOKEN获取redis中的用户
				String key = RedisConstants.LOGIN_USER_KEY + token;
				Map<Object, Object> usermap = stringRedisTemplate.opsForHash()
								.entries(key);
				//3.判断用户是否存在
				if (usermap.isEmpty()) {
						return true;
				}
				// TODO: 2022/11/18 5.将查询到的Hash数据转为UserDto对象
				UserDTO userDTO = BeanUtil.fillBeanWithMap(usermap, new UserDTO(), false);
				// TODO: 2022/11/18 6.存在,保存用户信息到ThreadLocal
				UserHolder.saveUser(userDTO);
				// TODO: 2022/11/18 7.刷新有效期
				stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
				//8.放行
				return true;
		}
		
		
		@Override
		public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
				//移除用户
				UserHolder.removeUser();
		}
}
