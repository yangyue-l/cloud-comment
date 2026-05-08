package com.yangyue.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangyue.entity.Blog;
import com.yangyue.mapper.BlogMapper;
import com.yangyue.service.IBlogService;

import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
