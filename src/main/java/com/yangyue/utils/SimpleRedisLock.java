package com.yangyue.utils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import cn.hutool.core.lang.UUID;

public class SimpleRedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;
    private String name;
    

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLCOK_SCRIPT;
    static{
        UNLCOK_SCRIPT = new DefaultRedisScript<>();
        UNLCOK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLCOK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String threadId = ID_PREFIX + Thread.currentThread().threadId();
        Boolean success = stringRedisTemplate.opsForValue()
                            .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }


     @Override
    public void unlock() {
       //调用lua脚本
       stringRedisTemplate.execute(UNLCOK_SCRIPT,Collections.singletonList(KEY_PREFIX + name),
                                     ID_PREFIX + Thread.currentThread().threadId());
    }


    // @Override
    // public void unlock() {
    //     //获取线程标识
    //     String threadId = ID_PREFIX + Thread.currentThread().threadId();
    //     //获取锁标识
    //     String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
    //     if(threadId.equals(id)){
    //         stringRedisTemplate.delete(KEY_PREFIX + name);
    //     }
    // }

}
