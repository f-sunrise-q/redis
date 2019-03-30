package com.advance.redis;

import com.advance.redis.annotation.SeashellCache;
import com.advance.redis.service.RedisPropertyServiceImpl;
import com.advance.redis.service.TestService;
import com.advance.redis.service.impl.RedisTemplateService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by qin on 19/3/31.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {WebApplication.class})
public class AnnotationTest {

    @Autowired
    private RedisPropertyServiceImpl redisPropertyService;

    @Autowired
    private RedisTemplateService redisTemplateService;

    @Autowired
    private TestService testService;

    @Test
    public void testInsertObject(){
        testService.getTestBo("aaa", 1);
    }
}
