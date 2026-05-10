package com.yangyue.utils;

public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec
     * @return true成功,false失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
