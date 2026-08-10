package com.dietscheduler.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * A small, bounded, named thread pool for the outbound Open Food Facts fan-out in
 * {@code ExternalRecipeService.getDetail} (up to 20 concurrent lookups per external-recipe
 * preview). Previously this used {@code CompletableFuture.supplyAsync}'s default executor, the
 * JVM-wide common ForkJoinPool -- blocking HTTP calls on that shared pool starve every other
 * unrelated {@code CompletableFuture}/parallel stream in the process. Bounded so N concurrent
 * requests from different users can't add up to an unbounded number of threads either.
 */
@Configuration
public class AsyncConfig {

    @Bean
    public Executor externalApiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("external-api-");
        executor.initialize();
        return executor;
    }
}
