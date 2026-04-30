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
        // Java 21 virtual threads — lightweight, no need to size a pool
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}