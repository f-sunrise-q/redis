package com.advance.redis.service;

import com.advance.redis.TestBo;
import com.advance.redis.annotation.SeashellCache;
import com.advance.redis.annotation.SeashellCacheConstant;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @SeashellCache(infix = "test", key = "setTest", expire = 50000, valueType = SeashellCacheConstant.VALUE_TYPE_SET)
    public Set<String> addSet2Redis(){
        Set<String> set= new HashSet<>();
        set.add("123");
        set.add("abc");
        return set;
    }

    @SeashellCache(infix = "test", key = "listTest", expire = 50000, refresh = true, valueType = SeashellCacheConstant.VALUE_TYPE_LIST)
    public List<String> addList2Redis(){
        List<String> list = new ArrayList<>();
        list.add("hh");
        list.add("dd");
        return list;
    }
}
