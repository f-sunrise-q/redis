package com.advance.redis.service.impl;

import com.advance.redis.config.IRedisPropertyService;
import com.advance.redis.service.IRedisTemplateService;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于spring和redis的redisTemplate工具类封装
 * 针对所有的map 都是以hm开头的方法
 * 针对所有的Set 都是以s开头的方法
 * 针对所有的List 都是以l开头的方法
 *
 * @author gonghao
 * @modify by fangqin 2019/2/27
 * 引入泛型、重构map、set、list各种操作方法
 */
@Service("redisTemplateService")
public class RedisTemplateService implements IRedisTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(RedisTemplateService.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired(required = false)
    private IRedisPropertyService redisPropertyService;

    private String keyPrefix = null;

    /**
     * 统一设置key的前缀
     *
     * @param key
     * @return
     */
    @Override
    public String setKeyPrefix(String key) {
        if (Strings.isNullOrEmpty(keyPrefix)) {
            if (redisPropertyService != null) {
                String prefix = redisPropertyService.getKeyPrefix();
                if (!Strings.isNullOrEmpty(prefix)) {
                    keyPrefix = prefix;
                }
            }
        }

        if (!Strings.isNullOrEmpty(keyPrefix)) {
            //如果不是以默认前缀开头的，则添加前缀
            if (!key.startsWith(keyPrefix)) {
                key = keyPrefix + ":" + key;
            }

        }

        return key;
    }


    //=============================common============================

    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     * @return
     */
    @Override
    public boolean expire(String key, long time) {
        key = setKeyPrefix(key);
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            logger.error("expire cache error:", e);
            return false;
        }
    }

    /**
     * 根据key 获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    @Override
    public long getExpire(String key) {
        key = setKeyPrefix(key);
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    @Override
    public boolean hasKey(String key) {
        key = setKeyPrefix(key);
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            logger.error("check key exist error, -[key = {}]", key, e);
            return false;
        }
    }


 

 


    //============================String=============================

    /**
     * mod by fangqin 2019/2/27
     * 底层方法不希望抛出任何异常中断service中方法的执行，连接redis失败就正常返回null
     * <p>
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    @Override
    public <T> T get(String key) {
        try {
            ValueOperations<String, T> valueOperations = redisTemplate.opsForValue();
            if (!Strings.isNullOrEmpty(key) && valueOperations != null) {
                return valueOperations.get(setKeyPrefix(key));
            }
        } catch (Exception e) {
            logger.error("get value from cache error, -[key={}]", key);
        }
        return null;
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    @Override
    public <T> boolean set(String key, T value) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                redisTemplate.opsForValue().set(setKeyPrefix(key), value);
                return true;
            }
        } catch (Exception e) {
            logger.error("set cache error, -[key={},value={}]", key, value, e);
        }
        return false;
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    @Override
    public <T> boolean set(String key, T value, long time) {
        key = setKeyPrefix(key);
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error("set cache error,-[key={},value={},time={}]", key, value, time, e);
            return false;
        }
    }

   
    


   



   
    

   

  


   
  

    /**
     * 删除多个key
     *
     * @param keys
     * @return
     */
    @Override
    public boolean remove(String... keys) {
        try {
            if (keys != null && keys.length > 0) {
                redisTemplate.delete(Arrays.stream(keys).map(this::setKeyPrefix).collect(Collectors.toList()));
            }
            return true;
        } catch (Exception e) {
            logger.error("remove from cache error, -[keys={}]", keys, e);
        }
        return false;
    }

    /**
     * 根据前缀批量删除
     *
     * @param key
     * @return
     */
    @Override
    public boolean batchDelete(String key) {
        try {
            if (Strings.isNullOrEmpty(key)) {
                key = "*";
            }
            String keyPrex = setKeyPrefix(key);

            Set keys = redisTemplate.keys(keyPrex);
            if (!CollectionUtils.isEmpty(keys)) {
                redisTemplate.delete(keys);
            }
            return true;
        } catch (Exception e) {
            logger.error("del from cache error,-[key={}]", key, e);
        }
        return false;
    }

    /**
     * 获取key对应value的类型
     *
     * @param key
     * @return
     */
    @Override
    public String getType(String key) {
        try {
            DataType dataType = redisTemplate.type(setKeyPrefix(key));
            if (dataType != null) {
                return dataType.name();
            }
        } catch (Exception e) {
            logger.error("get type from cache error,-[key={}]", key, e);
        }
        return null;
    }

    /**
     * List相关操作
     *
     * @param key
     */
    @Override
    public <T> List<T> getList(String key) {
        try {
            ListOperations<String, T> listOperations = redisTemplate.opsForList();
            if (!Strings.isNullOrEmpty(key) && listOperations != null) {
                return listOperations.range(setKeyPrefix(key), 0, -1);
            }
        } catch (Exception e) {
            logger.error("get list from cache error, -[key = {}]", key, e);
        }
        return null;
    }

    @Override
    public <T> boolean setList(String key, List<T> value) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                String realKey = setKeyPrefix(key);
                redisTemplate.opsForList().rightPushAll(realKey, value);
                return true;
            }
        } catch (Exception e) {
            logger.error("set list to cache error, - [key = {}, value = {}]", key, value, e);
        }
        return false;
    }

    @Override
    public <T> boolean setList(String key, List<T> value, long time) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                String realKey = setKeyPrefix(key);
                redisTemplate.opsForList().rightPushAll(realKey, value);
                if (time > 0) {
                    redisTemplate.expire(realKey, time, TimeUnit.SECONDS);
                }
                return true;
            }
        } catch (Exception e) {
            logger.error("set list to cache error, -[key={},value={}]", key, value, e);
        }
        return false;
    }

    @Override
    public <T> boolean setListItem(String key, T... value) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                redisTemplate.opsForList().rightPushAll(setKeyPrefix(key), value);
                return true;
            }
        } catch (Exception e) {
            logger.error("set list to cache error,-[key={},value={}]", key, value, e);
        }
        return false;
    }

    /**
     * Set相关操作
     *
     * @param key
     */
    @Override
    public <T> Set<T> getSet(String key) {
        try {
            SetOperations<String, T> setOperations = redisTemplate.opsForSet();
            if (!Strings.isNullOrEmpty(key) && setOperations != null) {
                return setOperations.members(setKeyPrefix(key));
            }
        } catch (Exception e) {
            logger.error("get set from cache error,-[key={}]", key, e);
        }
        return null;
    }

    @Override
    public <T> boolean setSet(String key, Set<T> value) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                redisTemplate.opsForSet().add(setKeyPrefix(key), value.toArray());
                return true;
            }
        } catch (Exception e) {
            logger.error("set Set from cache error,-[key={},value={}]", key, value, e);
        }
        return false;
    }

    @Override
    public <T> boolean setSet(String key, Set<T> value, long time) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                String realKey = setKeyPrefix(key);
                redisTemplate.opsForSet().add(realKey, value.toArray());
                if (time > 0) {
                    redisTemplate.expire(realKey, time, TimeUnit.SECONDS);
                }
                return true;
            }
        } catch (Exception e) {
            logger.error("set Set from cache error,-[key={},value={}]", key, value, e);
        }
        return false;
    }

    @Override
    public boolean setSetItem(String key, Object... value) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                redisTemplate.opsForSet().add(setKeyPrefix(key), value);
                return true;
            }
        } catch (Exception e) {
            logger.error("set Set from cache error,-[key={},value={}]", key, value, e);
        }
        return false;
    }

    @Override
    public <T> boolean removeInSet(String key, T... value) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                redisTemplate.opsForSet().remove(setKeyPrefix(key), value);
                return true;
            }
        } catch (Exception e) {
            logger.error("remove Set from cache error,-[key={},value={}]", key, value, e);
        }
        return false;
    }

    @Override
    public <T> boolean existInSet(String key, T value) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                return redisTemplate.opsForSet().isMember(setKeyPrefix(key), value);
            }
        } catch (Exception e) {
            logger.error("remove Set from cache error,-[key={},value={}]", key, value, e);
        }
        return false;
    }

    /**
     * Map相关操作
     *
     * @param key
     */
    @Override
    public <K, V> Map<K, V> getMap(String key) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                HashOperations<String, K, V> mapOperations = redisTemplate.opsForHash();
                return mapOperations.entries(setKeyPrefix(key));
            }
        } catch (Exception e) {
            logger.error("get Map from cache error,-[key={}]", key, e);
        }
        return null;
    }

    @Override
    public <K, V> boolean setMap(String key, Map<K, V> value) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                redisTemplate.opsForHash().putAll(setKeyPrefix(key), value);
                return true;
            }
        } catch (Exception e) {
            logger.error("get Map from cache error,-[key={},value={}]", key, value, e);
        }
        return false;
    }

    @Override
    public <K, V> boolean setMap(String key, Map<K, V> value, long time) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                String realKey = setKeyPrefix(key);
                redisTemplate.opsForHash().putAll(realKey, value);
                if (time > 0) {
                    redisTemplate.expire(realKey, time, TimeUnit.SECONDS);
                }
                return true;
            }
        } catch (Exception e) {
            logger.error("get Map from cache error,-[key={}, value={}, time={}]", key, value, time, e);
        }
        return false;
    }

    @Override
    public <K, V> V getMapItem(String key, K mapKey) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                HashOperations<String, K, V> mapOperation = redisTemplate.opsForHash();
                return mapOperation.get(setKeyPrefix(key), mapKey);
            }
        } catch (Exception e) {
            logger.error("get Map from cache error,-[key={}, mapKey={}]", key, mapKey, e);
        }
        return null;
    }

    @Override
    public <K, V> boolean setMapItem(String key, K mapKey, V value) {
        try {
            if (!Strings.isNullOrEmpty(key)) {

                redisTemplate.opsForHash().put(setKeyPrefix(key), mapKey, value);
                return true;
            }
        } catch (Exception e) {
            logger.error("get Map from cache error,-[key={}, value={}]", key, value, e);
        }
        return false;
    }

    @Override
    public <K> boolean removeInMap(String key, K... mapKey) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                redisTemplate.opsForHash().delete(setKeyPrefix(key), mapKey);
                return true;
            }
        } catch (Exception e) {
            logger.error("get Map from cache error,-[key={}, mapKey={}]", key, mapKey, e);
        }
        return false;
    }

    @Override
    public <K> boolean existsInMap(String key, K mapKey) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                return redisTemplate.opsForHash().hasKey(setKeyPrefix(key), mapKey);
            }
        } catch (Exception e) {
            logger.error("get Map from cache error,-[key={}, mapKey={}]", key, mapKey, e);
        }
        return false;
    }

    @Override
    public Long getListSize(String key) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                return redisTemplate.opsForList().size(setKeyPrefix(key));
            }
        } catch (Exception e) {
            logger.error("get list size from cache error,-[key={}]", key, e);
        }
        return 0L;
    }

    @Override
    public Long getSetSize(String key) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                return redisTemplate.opsForSet().size(setKeyPrefix(key));
            }
        } catch (Exception e) {
            logger.error("get set size from cache error,-[key={}]", key, e);
        }
        return 0L;
    }

    @Override
    public Long getMapSize(String key) {
        try {
            if (!Strings.isNullOrEmpty(key)) {
                return redisTemplate.opsForHash().size(setKeyPrefix(key));
            }
        } catch (Exception e) {
            logger.error("get map size from cache error,-[key={}]", key, e);
        }
        return 0L;
    }
}
