package com.yangyue.config;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import com.yangyue.dto.UserDTO;
import com.yangyue.utils.RedisConstants;
import com.yangyue.utils.UserHolder;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

public class RefreshTokenInterceptor implements HandlerInterceptor{

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        
        UserHolder.removeUser();
    }
        

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
            
            return true;
        }
        
        String key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object,Object> userMap = stringRedisTemplate.opsForHash()
                                    .entries(RedisConstants.LOGIN_USER_KEY + token);
        if(userMap.isEmpty()){
            return true;
        }

        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //存在则保存到ThreadLocal
        UserHolder.saveUser(userDTO);
        //TODO 将单位改成秒
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL,TimeUnit.MINUTES);

        return true;

    }
}
