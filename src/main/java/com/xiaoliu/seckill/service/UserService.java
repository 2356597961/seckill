package com.xiaoliu.seckill.service;

import com.xiaoliu.seckill.error.BusinessException;
import com.xiaoliu.seckill.service.model.UserModel;

/**
 * Created by xiaoliu on 2022/4/18.
 */
public interface UserService {
    //通过用户ID获取用户对象的方法
    UserModel getUserById(Integer id);

    //通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);

    void register(UserModel userModel) throws BusinessException;

    /*
    telphone:用户注册手机
    password:用户加密后的密码
     */
    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
