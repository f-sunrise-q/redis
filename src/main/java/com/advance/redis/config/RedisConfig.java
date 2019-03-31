package com.advance.redis.config;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.support.NullValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * <p>
 * redis配置
 * </p>
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties({RedisPropertiesPlus.class})
public class RedisConfig extends CachingConfigurerSupport {

    public static final int EVICTION_TIME = 60000;

    public static final int MIN_EVICTABLEIDLE_TIME = -1;

    public static final int SOFT_EVICTABLEIDLE_TIME = 180000;

    @Autowired(required = false)
    private RedisPropertiesPlus redisPropertiesPlus;

    @Autowired(required = false)
    private IRedisPropertyService redisPropertyService;

    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        RedisPropertiesPlus redisProperties = this.getRedisProperties();
        if (redisProperties == null) {
            redisProperties = new RedisPropertiesPlus();
        }
        if (redisProperties.getPool() == null) {
            redisProperties.setPool(new RedisProperties.Pool());
        }

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //最大能够保持idle状态的jedis实例数
        jedisPoolConfig.setMaxIdle(redisProperties.getPool().getMaxIdle());
        //最大分配对象数,如果赋值为-1，则表示不限制
        jedisPoolConfig.setMaxTotal(redisProperties.getPool().getMaxActive());
        //最小能够保持idle状态的jedis实例数
        jedisPoolConfig.setMinIdle(redisProperties.getPool().getMinIdle());
        //当池内没有返回对象时，最大等待时间
        jedisPoolConfig.setMaxWaitMillis(redisProperties.getPool().getMaxWait().toMillis());

        //当调用borrow Object方法时，是否进行有效性检查 ,如果为true，则得到的jedis实例均是可用的
        jedisPoolConfig.setTestOnBorrow(redisProperties.isTestOnBorrow());
        jedisPoolConfig.setTestWhileIdle(redisProperties.isTestWhileIdle());
        jedisPoolConfig.setTestOnReturn(redisProperties.isTestOnReturn());
        jedisPoolConfig.setTestOnCreate(redisProperties.isTestOnCreate());

        //一个对象至少停留在idle状态的最短时间，然后才能被idle object evitor扫描并驱逐；这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(EVICTION_TIME);
        //表示idle object evitor两次扫描之间要sleep的毫秒数
        jedisPoolConfig.setMinEvictableIdleTimeMillis(MIN_EVICTABLEIDLE_TIME);
        jedisPoolConfig.setSoftMinEvictableIdleTimeMillis(SOFT_EVICTABLEIDLE_TIME);

        JedisConnectionFactory redisConnectionFactory = new JedisConnectionFactory();
        //redis的连接信息，需要组件提供
        redisConnectionFactory.setHostName(redisProperties.getHost());
        redisConnectionFactory.setPort(redisProperties.getPort());
        if (redisProperties.getPassword() != null) {
            redisConnectionFactory.setPassword(redisProperties.getPassword());
        }
        redisConnectionFactory.setUsePool(true);
        redisConnectionFactory.setPoolConfig(jedisPoolConfig);

        //允许修改默认数据库
        redisConnectionFactory.setDatabase(redisProperties.getDatabase());
        //设置超时时间
        if (redisProperties.getTimeout() > 0) {
            redisConnectionFactory.setTimeout(redisProperties.getTimeout());
        }

        redisConnectionFactory.afterPropertiesSet();

        return redisConnectionFactory;
    }

    private static class NullValueSerializer extends StdSerializer<NullValue> {
        private static final long serialVersionUID = 1999052150548658808L;
        private final String classIdentifier;

        NullValueSerializer(String classIdentifier) {
            super(NullValue.class);
            this.classIdentifier = StringUtils.hasText(classIdentifier) ? classIdentifier : "@class";
        }

        @Override
        public void serialize(NullValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField(this.classIdentifier, NullValue.class.getName());
            jgen.writeEndObject();
        }
    }


    @Bean
    public GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule((new SimpleModule()).addSerializer(new NullValueSerializer(null)));
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    public RedisTemplate redisTemplate(@Autowired GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(genericJackson2JsonRedisSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(genericJackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory());
        return stringRedisTemplate;
    }

    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator() {
            /**
             * 对参数进行拼接后MD5
             */
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder sb = new StringBuilder();
                sb.append(target.getClass().getName());
                sb.append(':').append(method.getName());

                StringBuilder paramsSb = new StringBuilder();
                for (Object param : params) {
                    // 如果不指定，默认生成包含到键值中
                    if (param != null) {
                        paramsSb.append(param.toString());
                    }
                }

                if (paramsSb.length() > 0) {
                    sb.append("_").append(paramsSb);
                }
                return sb.toString();
            }

        };
    }

    private RedisPropertiesPlus getRedisProperties() {
        if (redisPropertyService != null && redisPropertyService.getRedisProperties() != null) {
            return redisPropertyService.getRedisProperties();
        }
        return redisPropertiesPlus;
    }
}
