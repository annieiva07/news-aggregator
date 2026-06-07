package com.newsaggregator.service;

import com.newsaggregator.parser.ParserManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SchedulerService {

    private final ParserManager parserManager;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> task;
    private boolean running = false;
    private int intervalMinutes = 30;

    public SchedulerService(ParserManager parserManager) {
        this.parserManager = parserManager;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "news-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public void start(int intervalMinutes) {
        if (intervalMinutes <= 0) {
            System.out.println("Ошибка: интервал должен быть положительным числом.");
            return;
        }
        if (running) {
            stop();
        }
        this.intervalMinutes = intervalMinutes;
        task = executor.scheduleAtFixedRate(
                this::update,
                intervalMinutes,
                intervalMinutes,
                TimeUnit.MINUTES
        );
        running = true;
        System.out.println("Автообновление запущено: каждые " + intervalMinutes + " минут.");
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
        }
        running = false;
        System.out.println("Автообновление остановлено.");
    }

    public void shutdown() {
        stop();
        executor.shutdown();
    }

    public boolean isRunning() {
        return running;
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    private void update() {
        System.out.println("\n[Автообновление] Запуск сбора новостей...");
        parserManager.parseAndSave();
    }
}
