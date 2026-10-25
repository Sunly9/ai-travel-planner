package org.example.travel_project1.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SSE 专用线程池
 *
 * 以前每个流式请求都 new Thread()，本地点几下就堆一堆线程，也没有上限。
 * 现在固定池 + 有界队列 + 拒绝策略（满了由调用线程自己跑，避免直接丢请求）。
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "sseExecutor", destroyMethod = "shutdown")
    public ExecutorService sseExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                4,                                  // 核心线程
                16,                                 // 最大线程
                60L, TimeUnit.SECONDS,               // 空闲回收
                new ArrayBlockingQueue<>(200),       // 有界队列
                new CustomizableThreadFactory("sse-"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
