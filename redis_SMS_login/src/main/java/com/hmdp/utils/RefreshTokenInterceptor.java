package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * 拦截一切页面，进行redis中token的刷新
 *
 * @author 小如
 * @date 2023/08/03
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate redisTemplate) {
        this.stringRedisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取请求头中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
           return true;
        }

        String key = RedisConstants.LOGIN_USER_KEY + token;
        //2.基于token获取redis中的用户
        Map<Object, Object> user = stringRedisTemplate.opsForHash()
                .entries(key);
        //3.判断用户是否存在
        if(user.isEmpty()){
           return true;
        }
        //5.存在，转化成UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(user, new UserDTO(), false);

        UserHolder.saveUser(userDTO);
        //  刷新token有效期
        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //放行
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
