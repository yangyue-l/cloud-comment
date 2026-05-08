package com.yangyue.service;


import javax.servlet.http.HttpSession;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yangyue.dto.LoginFormDTO;
import com.yangyue.dto.Result;
import com.yangyue.entity.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

}
