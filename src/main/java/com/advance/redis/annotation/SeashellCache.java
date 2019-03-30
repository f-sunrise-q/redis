package com.advance.redis.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SeashellCache {
    /**
     * 缓存key的中缀字符串
     *
     * @return
     */
    String infix() default "";

    /**
     * key的EL表达式
     *
     * @return
     */
    String key() default "#p";

    /**
     * 过期时间，单位秒
     *
     * @return
     */
    long expire() default 0;

    /**
     * 是否强制刷新缓存，默认为false
     *
     * @return
     */
    boolean refresh() default false;

    /**
     * 数据类型，支持string、set、list、map
     *
     * @return
     */
    String valueType() default SeashellCacheConstant.VALUE_TYPE_STRING;
}
