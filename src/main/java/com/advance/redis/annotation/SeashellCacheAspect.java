package com.advance.redis.annotation;

import com.advance.redis.service.IRedisTemplateService;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

@Aspect
@Component
public class SeashellCacheAspect {

    private static final Logger logger = LoggerFactory.getLogger(SeashellCacheAspect.class);

    private volatile Set<String> existKeySet = Sets.newConcurrentHashSet();

    private volatile Map<String, Expression> expressionMap = Maps.newConcurrentMap();

    @Autowired
    private IRedisTemplateService redisTemplateService;

    @Pointcut("@within(com.advance.redis.annotation.SeashellCache)||@annotation(com.advance.redis.annotation.SeashellCache) ")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object checkExistInCache(ProceedingJoinPoint point) throws Throwable {

        // 获得切入方法参数
        Object[] args = point.getArgs();
        //获取SeashellCache注解
        Annotation annotation = getSeashellCacheAnno(point);
        //是否强制更新缓存
        //todo list的强制更新有点儿问题,refresh=true时依然会插入多次
        if (annotation != null && !((SeashellCache) annotation).refresh()) {

            //获取redis key
            String key = this.getCacheKey(annotation, args);

            String valueType = ((SeashellCache) annotation).valueType();
            Object result = null;
            switch (valueType) {
                case SeashellCacheConstant.VALUE_TYPE_LIST:
                    result = redisTemplateService.getList(key);
                    break;
                case SeashellCacheConstant.VALUE_TYPE_MAP:
                    result = redisTemplateService.getMap(key);
                    break;
                case SeashellCacheConstant.VALUE_TYPE_SET:
                    result = redisTemplateService.getSet(key);
                    break;
                default:
                    result = redisTemplateService.get(key);

            }

            if (!isEmpty(result)) {
                existKeySet.add(key);
                return result;
            }

        }

        //继续执行原方法
        return point.proceed();
    }


    private boolean isEmpty(Object obj){
        if(obj == null){
            return true;
        }
        if(obj instanceof  Collection){
            return ((Collection) obj).isEmpty();
        }

        if(obj instanceof Map){
            return ((Map) obj).isEmpty();
        }
        if(obj instanceof String){
            return ((String) obj).isEmpty();
        }
        return false;
    }

    @AfterReturning(value = "pointcut()", returning = "result")
    public void after(JoinPoint point, Object result) {

        Annotation annotation = getSeashellCacheAnno(point);
        if (annotation != null) {
            String key = getCacheKey(annotation, point.getArgs());

            if (!Strings.isNullOrEmpty(key)) {
                //由于list的set操作是追加而不是覆盖，多次set会插入多次，通过判断是否已存在避免重复插入
                if (!existKeySet.isEmpty() && existKeySet.contains(key)) {
                    existKeySet.remove(key);
                    return;
                }

                //为了解决list同一值插入多次问题，修改为覆盖，先删除再插入
                String valueType = ((SeashellCache) annotation).valueType();
                switch (valueType) {
                    case SeashellCacheConstant.VALUE_TYPE_LIST:
                        if (result instanceof List) {
                            redisTemplateService.remove(key);
                            redisTemplateService.setList(key, (List) result);
                        }
                        break;
                    case SeashellCacheConstant.VALUE_TYPE_MAP:
                        if (result instanceof Map) {
                            redisTemplateService.remove(key);
                            redisTemplateService.setMap(key, (Map) result);
                        }
                        break;
                    case SeashellCacheConstant.VALUE_TYPE_SET:
                        if (result instanceof Set) {
                            redisTemplateService.remove(key);
                            redisTemplateService.setSet(key, (Set) result);
                        }
                        break;
                    default:
                        redisTemplateService.set(key, result);

                }

                if (((SeashellCache) annotation).expire() > 0) {
                    redisTemplateService.expire(key, ((SeashellCache) annotation).expire());
                }
            }

        }
    }

    /**
     * 获取SeashellCache注解
     *
     * @param point
     * @return
     */
    private Annotation getSeashellCacheAnno(JoinPoint point) {

        // 获得切入的方法
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        Annotation[] methodAnnotations = method.getAnnotations();
        if (methodAnnotations != null && methodAnnotations.length > 0) {
            for (Annotation annotation : methodAnnotations) {
                if (annotation instanceof SeashellCache) {
                    return annotation;
                }
            }
        }
        return null;
    }

    /**
     * 生成缓存key
     *
     * @param annotation
     * @param args
     * @return
     */
    private String getCacheKey(Annotation annotation, Object[] args) {
        String infix = ((SeashellCache) annotation).infix();

        String key = genKeyByExpression(((SeashellCache) annotation).key(), args);
        if (!Strings.isNullOrEmpty(infix)) {
            key = infix + ":" + key;
        }
        return key;

    }

    /**
     * 通过表达式生成，若key不是表达式直接返回key
     *
     * @param expression
     * @param args
     * @return
     */
    public String genKeyByExpression(String expression, Object[] args) {
        try {
            Expression cacheExpression = null;
            if (expressionMap.containsKey(expression)) {
                cacheExpression = expressionMap.get(expression);
            } else {
                SpelParserConfiguration spelParserConfiguration = new SpelParserConfiguration(true, true);
                SpelExpressionParser parser = new SpelExpressionParser(spelParserConfiguration);
                cacheExpression = parser.parseExpression(expression);
                expressionMap.put(expression, cacheExpression);
            }

            RootObject rootObject = new RootObject();
            if (args != null && (args.length > 0)) {
               for(Object o :args){
                   rootObject.getP().add(o);
               }
            }

            StandardEvaluationContext context = new StandardEvaluationContext(rootObject);
//            if (param != null) {
//                Object[] afterDealParams = new Object[param.length];
//                for (int i = 0; i < param.length; i++) {
//                    if (param[i] == null) {
//                        afterDealParams[i] = "";
//                    } else {
//                        afterDealParams[i] = param[i];
//                    }
//                }
//                context.setVariable("p", afterDealParams);
//            }
            String result = cacheExpression.getValue(context,String.class);
            return result != null ? result.toString() : "111";
        } catch (Exception e) {
            logger.error("generate key by expression error, - [expression={}] ", expression, e);
        }
        return expression;
    }

    public class RootObject {
        List<Object> p = new ArrayList<>();

        public List<Object> getP() {
            return p;
        }
    }

    public static void main(String[] args) {
        SeashellCacheAspect seashellCacheAspect = new SeashellCacheAspect();
        System.out.println(seashellCacheAspect.genKeyByExpression("p[0]", new Object[]{null}));
    }
}
