package com.jun.javautilrepo.common;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @decription: 常规测试
 * @date: 2022/6/8 11:12
 * @author: longjunjie@xinpayroll.com
 * @Since:
 */
@Slf4j
public class CommonTest {

    @Test
    public void test() {
        log.info("这是一个普通测试打印");
    }

    @Test
    public void testLeakBucket() {


    }

}