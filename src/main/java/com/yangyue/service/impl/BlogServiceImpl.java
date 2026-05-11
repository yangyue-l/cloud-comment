package com.yangyue.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangyue.dto.Result;
import com.yangyue.entity.Blog;
import com.yangyue.entity.User;
import com.yangyue.mapper.BlogMapper;
import com.yangyue.service.IBlogService;
import com.yangyue.service.IUserService;
import com.yangyue.utils.SystemConstants;
import com.yangyue.utils.UserHolder;

import cn.hutool.core.util.BooleanUtil;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.isBlogLiked(blog);
            this.queryBlogUser(blog);
        });

        return Result.ok(records);
    }


    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }


    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if(blog == null){
            return Result.fail("笔记不存在");
        }

        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }


    private void isBlogLiked(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        String key = "blog:like:" + blog.getId();
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId);
        blog.setIsLike(BooleanUtil.isTrue(isMember));
    }


    @Override
    public Result likeBlog(Long id) {
       Long userId = UserHolder.getUser().getId();
       String key = "blog:like:" + id;
       Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId);
       if(BooleanUtil.isFalse(isMember)){
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if(isSuccess){
                stringRedisTemplate.opsForSet().add(key, userId.toString());
            }
       }else{
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if(isSuccess){
                stringRedisTemplate.opsForSet().remove(key, userId.toString());
            }
       }
       

       return Result.ok();
    }

    

}
