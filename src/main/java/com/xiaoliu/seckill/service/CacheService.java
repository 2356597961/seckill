package com.xiaoliu.seckill.service;

/**
 * Created by xiaoliu on 2022/4/18.
 */
//封装本地缓存操作类
public interface CacheService {
    //存方法
    void setCommonCache(String key, Object value);

    //取方法
    Object getFromCommonCache(String key);
}
