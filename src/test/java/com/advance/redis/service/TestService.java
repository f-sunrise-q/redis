package com.advance.redis.service;

import com.advance.redis.TestBo;
import com.advance.redis.annotation.SeashellCache;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qin on 19/3/31.
 */

@Service
public class TestService {

    @SeashellCache(infix = "test", key = "#p[0]+'_'+#p[1]", expire = 5000)
    public TestBo getTestBo(String p1, Integer p2){
        TestBo bo = new TestBo();
        bo.setParam1(p1);
        bo.setParam2(p2);
        List<String> list = new ArrayList<>();
        list.add("hh");
        list.add("老汪是大猪蹄子");
        bo.setParam3(list);
        return bo;
    }
}
