package com.hmdp;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@MapperScan("com.hmdp.mapper")
@SpringBootApplication
@Slf4j
@EnableTransactionManagement
@EnableAspectJAutoProxy(exposeProxy = true)//暴露代理类对象
public class HmDianPingApplication {
		
		public static void main(String[] args) {
				SpringApplication.run(HmDianPingApplication.class, args);
				log.info("杨旭彪的项目启动咯");
		}
		
}
