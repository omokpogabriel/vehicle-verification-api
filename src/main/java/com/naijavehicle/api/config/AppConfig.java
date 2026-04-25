package com.naijavehicle.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;


@Configuration
public class AppConfig {

    @Bean("customThreadPool")
    public AsyncTaskExecutor customThreadPool() {
        // Wrapping the Virtual Thread Executor in a Spring Adapter
        // allows it to integrate better with Spring's Task execution framework.
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}