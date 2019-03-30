package com.advance.redis.service;

import com.advance.redis.config.IRedisPropertyService;
import com.advance.redis.config.RedisPropertiesPlus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by qin on 19/3/31.
 */

@Service
public class RedisPropertyServiceImpl implements IRedisPropertyService {

    @Autowired
    private RedisPropertiesPlus redisPropertiesPlus;

    @Override
    public RedisPropertiesPlus getRedisProperties() {
        if(redisPropertiesPlus==null) {
            redisPropertiesPlus = new RedisPropertiesPlus();
        }
        redisPropertiesPlus.setHost("127.0.0.1");
        redisPropertiesPlus.setPort(6379);
        return redisPropertiesPlus;
    }

    @Override
    public String getKeyPrefix() {
        return "advance.test";
    }
}
