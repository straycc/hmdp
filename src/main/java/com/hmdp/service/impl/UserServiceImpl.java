package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private RedisTemplate redisTemplate;


    /**
     * 发送验证码
     *
     * @param phone
     * @return
     */
    @Override
    public Result sendCode(String phone) {
        //1.校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号无效");
        }
        //2.随机生成六位验证码
        String code = RandomUtil.randomString(6);
        //3.验证码保存到redis
        redisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY+phone,code,5, TimeUnit.MINUTES);
        return Result.ok();
    }

    /**
     * 登录功能
     *
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        String phone = loginForm.getPhone();
        if(phone == null || RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号有误!");
        }
        //2.校验验证码
        String code = loginForm.getCode();
        String cacheCode = (String) redisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY+phone);
        if(code == null || RegexUtils.isCodeInvalid(code) || !code.equals(cacheCode)){
            return Result.fail("验证码有误!");
        }
        //3.查询用户信息(数据库)
        User user = query().eq("phone", phone).one();
        //4.用户不存在，新建用户并存储
        if(user == null){
            user = new User();
            user.setPhone(phone);
            save(user);
        }
        //5.用户存在,用户信息保存到redis
        String token = UUID.randomUUID().toString().replace("-", "");
        String userKey = RedisConstants.LOGIN_USER_KEY + token;
        //用户信息存储到map
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //存储到reids
        redisTemplate.opsForHash().putAll(userKey,userMap);
        redisTemplate.expire(userKey,RedisConstants.LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(token);
    }

    /**
     * 登出功能
     * @return
     */
    @Override
    public Result logout(String token) {
        //1.判断用户是否为登录状态
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null || userDTO.getId() == null){
            return Result.fail("用户登录状态异常");
        }
        //删除当前用户数据
        UserHolder.removeUser();
        //删除redis中缓存的用户数据
        redisTemplate.delete(RedisConstants.LOGIN_USER_KEY + token);
        return Result.ok();
    }

    /**
     * 用户签到
     * @return
     */
    @Override
    public Result sign() {
        // 获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 获取当前日期
        LocalDateTime now = LocalDateTime.now();
        // 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyy:MM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
        // 获取今天是本月的第几天（offset = dayOfMonth - 1）
        int dayOfMonth = now.getDayOfMonth();
        // 写入redis完成签到 SETBIT key offset 1
        redisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

}
