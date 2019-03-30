package com.advance.redis.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "spring.redis"
)
public class RedisPropertiesPlus {
    private int database = 0;
    private String url;
    private String host = "localhost";
    private String password;
    private int port = 6379;
    private boolean ssl;
    private int timeout;
    private RedisProperties.Pool pool;
    private RedisProperties.Sentinel sentinel;
    private RedisProperties.Cluster cluster;

    private boolean testOnReturn = false;
    private boolean testOnBorrow = false;
    private boolean testOnCreate = false;
    private boolean testWhileIdle = true;

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public RedisProperties.Pool getPool() {
        return pool;
    }

    public void setPool(RedisProperties.Pool pool) {
        this.pool = pool;
    }

    public RedisProperties.Sentinel getSentinel() {
        return sentinel;
    }

    public void setSentinel(RedisProperties.Sentinel sentinel) {
        this.sentinel = sentinel;
    }

    public RedisProperties.Cluster getCluster() {
        return cluster;
    }

    public void setCluster(RedisProperties.Cluster cluster) {
        this.cluster = cluster;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public boolean isTestOnCreate() {
        return testOnCreate;
    }

    public void setTestOnCreate(boolean testOnCreate) {
        this.testOnCreate = testOnCreate;
    }

    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }
}
