package com.newsaggregator;

import com.newsaggregator.cli.ConsoleMenu;
import com.newsaggregator.parser.ParserManager;
import com.newsaggregator.service.AnalyticsService;
import com.newsaggregator.service.SchedulerService;
import com.newsaggregator.storage.DatabaseConnection;
import com.newsaggregator.storage.NewsRepository;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== News Aggregator ===");
        System.out.println("Запуск системы...");

        DatabaseConnection.getConnection();

        NewsRepository repository = new NewsRepository();
        ParserManager parserManager = new ParserManager(repository);
        AnalyticsService analyticsService = new AnalyticsService(repository);
        SchedulerService schedulerService = new SchedulerService(parserManager);

        ConsoleMenu menu = new ConsoleMenu(repository, parserManager, analyticsService, schedulerService);
        menu.start();

        schedulerService.shutdown();
        DatabaseConnection.closeConnection();
    }
}
