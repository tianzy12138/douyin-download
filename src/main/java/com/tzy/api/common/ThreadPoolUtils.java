package com.tzy.api.common;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtils {
    public static ThreadPoolExecutor getThreadPool(int threadQty, String threadGroupName) {
        return new ThreadPoolExecutor(threadQty,
                threadQty,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100000),
                new BasicThreadFactory.Builder().namingPattern(threadGroupName + "-%s").build()
        );
    }

    public static ThreadPoolExecutor getThreadPool(int threadQty, String threadGroupName, int queueSize) {
        return new ThreadPoolExecutor(threadQty,
                threadQty,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(queueSize),
                new BasicThreadFactory.Builder().namingPattern(threadGroupName + "-%s").build()
        );
    }

    public static void main(String[] args) {
        ThreadPoolExecutor tzy = getThreadPool(10, "tzy");
        for (int i = 0; i < 10; i++) {
            tzy.execute(() -> System.out.println(Thread.currentThread().getName()));
        }
    }
}
