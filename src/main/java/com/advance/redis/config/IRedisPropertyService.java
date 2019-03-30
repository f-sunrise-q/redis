package com.advance.redis.config;

/**
 * redis属性服务
 * 组件需实现该接口并配置相关信息
 *
 * @author gonghao
 * @create 2018-09-26 18:52
 **/
public interface IRedisPropertyService {

    /**
     * 获取redis配置信息并实现，主要在该方法中配置连接信息
     *
     * @return
     */
    RedisPropertiesPlus getRedisProperties();

    /**
     * 设置redis的key前缀
     *
     * @return
     */
    String getKeyPrefix();
}
