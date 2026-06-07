package com.newsaggregator.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:news_aggregator.db";
    private static Connection connection = null;

    private static String getDbUrl() {
        String prop = System.getProperty("db.url");
        return (prop != null && !prop.isEmpty()) ? prop : DEFAULT_DB_URL;
    }

    // Получить соединение с БД
    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(getDbUrl());
                System.out.println("Подключение к базе данных установлено");
                createTables();
            } catch (SQLException e) {
                System.err.println("Ошибка подключения к БД: " + e.getMessage());
            }
        }
        return connection;
    }

    // Закрыть соединение
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Соединение с БД закрыто");
            } catch (SQLException e) {
                System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    // Создать таблицы, если их нет
    private static void createTables() {
        String createNewsTable = """
            CREATE TABLE IF NOT EXISTS news (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                full_text TEXT,
                publish_date TIMESTAMP,
                source_url TEXT UNIQUE,
                source_name TEXT,
                category TEXT,
                keywords TEXT,
                media_urls TEXT,
                views INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createHistoryTable = """
            CREATE TABLE IF NOT EXISTS history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                news_id INTEGER,
                action TEXT,
                changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (news_id) REFERENCES news(id) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createNewsTable);
            stmt.execute(createHistoryTable);
            stmt.execute("DELETE FROM news WHERE source_url LIKE 'https://example.com/%'");
            System.out.println("Таблицы созданы или уже существуют");
        } catch (SQLException e) {
            System.err.println("Ошибка создания таблиц: " + e.getMessage());
        }
    }
}