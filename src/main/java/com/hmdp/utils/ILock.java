package com.hmdp.utils;

public interface ILock {
		
		/*
		 * 尝试获取锁
		 * */
		boolean trylock(int timeOutSec);
		
		/*
		* 释放锁
		* */
		void unlock();
		
}
