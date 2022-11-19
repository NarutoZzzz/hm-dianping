package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
		
		@Resource
		private StringRedisTemplate stringRedisTemplate;
		
		@Override
		public Result sendCode(String phone, HttpSession session) {
				//校验手机号
				if (RegexUtils.isPhoneInvalid(phone)) {
						//不符合,返回错误
						return Result.fail("手机号输入无效,请重新输入");
				}
				//符合,生成验证码
				String code = RandomUtil.randomNumbers(6);
				//保存到session
//				session.setAttribute("code", code);
				//保存到redis // set key value ex 120
				stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
				//发送验证码
				log.debug("发送短信验证码成功,验证码:{}", code);
				//返回成功
				return Result.ok();
		}
		
		@Override
		public Result Login(LoginFormDTO loginForm, HttpSession session) {
				//1.校验手机号码
				String phone = loginForm.getPhone();
				if (RegexUtils.isPhoneInvalid(phone)) {
						//1.1手机号不符合,则返回错误
						return Result.fail("手机号格式错误了,请重新输入");
				}
				//2.校验验证码
				// TODO: 2022/11/18 从redis获取验证码并校验
				String cachecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
				String code = loginForm.getCode();
				if (cachecode == null || !cachecode.equals(code)) {
						//3.不一致,报错误
						return Result.fail("验证码错误");
				}
				//4.一致,根据手机号查询用户 select * from tb_user where phone = ?
				//MybatisPlus的方法
				User user = query().eq("phone", phone).one();
				//5.判断用户是否存在
				if (user == null) {
						//6.不存在,创建新用户并保存
						user = createUserWithPhone(phone);
				}
				// TODO: 2022/11/18 保存用户信息到redis中
				//7.1生成token作为登录令牌
				String token = UUID.randomUUID().toString(true);
				//7.2将User对象转为HashMap存储
				UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
				Map<String, Object> usermap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
								CopyOptions.create()
												.setIgnoreNullValue(true)
												.setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
				//7.3存储数据到redis中
				String tokenKey = LOGIN_USER_KEY + token;
				stringRedisTemplate.opsForHash().putAll(tokenKey, usermap);
				//7.4设置token的有效期
				stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
				//8.返回token
				return Result.ok(token);
		}
		
		private User createUserWithPhone(String phone) {
				//1.创建用户
				User user = new User();
				user.setPhone(phone);
				user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
				//2.保存用户
				save(user);
				return user;
		}
}
