package com.jun.javautilrepo.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @decription: 漏桶算法
 * @date: 2022/6/8 11:37
 * @author: longjunjie@xinpayroll.com
 * @Since:
 */
@Slf4j
public class LeakyBucket {

    // 限制的速率
    private final double rate;
    // 桶的大小
    private final double burst;
    // 最近的刷新时间
    private long refreshTime;
    // 刷新时水的容量
    private double water;

    private static final ReentrantLock lock = new ReentrantLock();

    public LeakyBucket(double tps) {
        // double计算不精确
        this.rate = tps / 1000;
        this.burst = tps + 1;
        this.refreshTime = getTimeOfDay();
        this.water = 0.0;
    }

    /**
     * 刷新
     */
    public void refreshWater() {
        long now = getTimeOfDay();
        water = Math.max(0, water - (now - refreshTime) * rate);
        refreshTime = now;
    }

    public long getTimeOfDay() {
        return System.currentTimeMillis();
    }

    /**
     * 判断是否允许接收请求
     * @return
     */
    public boolean permissionGranted() {
        try {
            lock.lock();
            refreshWater();
            if (water < burst) {
                water++;
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }


    static class Test {
        private ConcurrentLinkedQueue<Integer> storeList = new ConcurrentLinkedQueue<>();
        private ConcurrentLinkedQueue<Integer> overflowsList = new ConcurrentLinkedQueue<>();
        // qps 10;
        private double tps = 2_000.0;
        private LeakyBucket leakyBucket = new LeakyBucket(tps);


        public static void main(String[] args) throws ExecutionException, InterruptedException {
            Test test = new Test();

            // 模拟客户端 往桶里加水
            ThreadPoolExecutor tpe = new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100),
                    Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

            List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();
            int queryTotal = 10_000;
            for(int n = 0; n < 100 ; n++) {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                for (int i = 0; i < queryTotal; i++) {
                    int data = i;
                    completableFutureList.add(CompletableFuture.runAsync(() -> test.uploadData(data), tpe));
                }
                CompletableFuture<Void> allTask = CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0]));
                allTask.thenAccept(v -> {
                    stopWatch.stop();
                });
                allTask.get();
                long times = stopWatch.getTotalTimeMillis();
                double curTps = Math.round((double) queryTotal / times * 1000);
                double acceptSum = Math.round((double)times / 1000 * test.tps);
                // 计算请求端的tps
                log.info("完成处理，耗时: {}ms，当前TPS：{}，限流程序在当前时间理论接受请求数量：{}，实际接受与理论差值：{}，接收数据大小：{}，未及时响应大小：{}", times,
                        curTps,
                        acceptSum,
                        test.storeList.size() - acceptSum,
                        test.storeList.size(), test.overflowsList.size());
                test.storeList.clear();
                test.overflowsList.clear();
            }
        }

        // 服务器接收数据限流程序
        public void uploadData(int data) {
            // 模拟请求延迟
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (leakyBucket.permissionGranted()) {
                storeList.add(data);
            } else {
                overflowsList.add(data);
            }
        }
    }


}
