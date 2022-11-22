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
public class LoginInterceptor implements HandlerInterceptor {
		
//		private StringRedisTemplate stringRedisTemplate;
//
//		public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
//				this.stringRedisTemplate = stringRedisTemplate;
//		}
		
		//前置拦截器
		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
				//1.判断是否需要拦截(ThreadLocal中是否有该用户)
				if (UserHolder.getUser() == null){
						//没有,需要拦截,设置状态码
						response.setStatus(401);
						//拦截
						return false;
				}
				//有用户,则放行
				return true;
		}
		
}
