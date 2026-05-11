package com.yangyue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yangyue.dto.Result;
import com.yangyue.entity.Blog;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result queryHotBlog(Integer current);

    Result likeBlog(Long id);

}
