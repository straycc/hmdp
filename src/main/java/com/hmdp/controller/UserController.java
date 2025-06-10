package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.xml.ws.Holder;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@Api(tags = "用户相关接口")
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @ApiOperation("发送验证码")
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        log.info("发送手机验证码");
        return userService.sendCode(phone);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @ApiOperation("登录")
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        userService.login(loginForm,session);
        return Result.ok();
    }

    /**
     * 登出功能
     * @return 无
     */
    @ApiOperation("登出")
    @PostMapping("/logout")
    public Result logout(String token){
        Long userId  = UserHolder.getUser().getId();
        log.info("用户:{} 退出登录",userId);
        return userService.logout(token);
    }

    /**
     * 查询登录用户信息
     * @return
     */
    @GetMapping("/me")
    @ApiOperation("查询登录用户信息")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO userDto = UserHolder.getUser();
        return Result.ok(userDto);
    }

    /**
     * 用户详细信息
     * @param userId
     * @return
     */
    @GetMapping("/info/{id}")
    @ApiOperation("用户详细信息")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    /**
     * 根据id查询用户信息
     * @param userId
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询用户信息")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }


    /**
     * 用户签到
     * @return
     */
    @PostMapping("/sign")
    @ApiOperation("用户签到")
    public Result sign(){
        return userService.sign();
    }
}
