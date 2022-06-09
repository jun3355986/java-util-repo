package com.jun.javautilrepo.common;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.util.StopWatch;

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

        for (int j = 0; j < 1_000; j++) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int n = 0;
            for (int i = 0; i < 100_000; i++) { n++;}
            stopWatch.stop();
            log.info(" 耗时ms:{}, n :{} 耗时nano：{}",  stopWatch.getTotalTimeMillis(), n, stopWatch.getTotalTimeNanos());
        }
        log.info("完成。。。。。。。。。");

    }

}