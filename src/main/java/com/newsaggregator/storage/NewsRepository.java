package com.newsaggregator.storage;

import com.newsaggregator.model.News;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NewsRepository {
    private final ObjectMapper objectMapper;

    public NewsRepository() {
        this.objectMapper = new ObjectMapper();
    }

    // Сохранить новость (если такой URL уже есть, не добавлять)
    public boolean save(News news) {
        String url = news.getSourceUrl();
        if (url == null || url.isEmpty() || url.startsWith("https://example.com/")) {
            return false;
        }
        String sql = """
            INSERT INTO news (title, description, full_text, publish_date, 
                              source_url, source_name, category, keywords, 
                              media_urls, views, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, news.getTitle());
            pstmt.setString(2, news.getDescription());
            pstmt.setString(3, news.getFullText());

            // Обработка даты
            if (news.getPublishDate() != null) {
                pstmt.setString(4, news.getPublishDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                pstmt.setString(4, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            pstmt.setString(5, news.getSourceUrl());
            pstmt.setString(6, news.getSourceName());
            pstmt.setString(7, news.getCategory());
            pstmt.setString(8, keywordsToJson(news.getKeywords()));
            pstmt.setString(9, mediaUrlsToJson(news.getMediaUrls()));
            pstmt.setInt(10, news.getViews());

            if (news.getCreatedAt() != null) {
                pstmt.setString(11, news.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                pstmt.setString(11, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            if (news.getUpdatedAt() != null) {
                pstmt.setString(12, news.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                pstmt.setString(12, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            int result = pstmt.executeUpdate();

            if (result > 0) {
                logHistory(news.getSourceUrl(), "ADDED");
            }

            return result > 0;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.out.println("Новость уже существует: " + news.getTitle());
            } else {
                System.err.println("Ошибка сохранения новости: " + e.getMessage());
            }
            return false;
        }
    }

    public List<News> findAll() {
        return findBySql("SELECT * FROM news ORDER BY publish_date ASC, id ASC", null);
    }

    public List<News> findAllSortedBy(String field) {
        String column = switch (field) {
            case "views" -> "views";
            case "source" -> "source_name";
            case "category" -> "category";
            default -> "publish_date";
        };
        return findBySql("SELECT * FROM news ORDER BY " + column + " DESC", null);
    }

    // Поиск по ключевым словам
    public List<News> searchByKeyword(String keyword) {
        String sql = "SELECT * FROM news WHERE title LIKE ? OR full_text LIKE ? OR description LIKE ? ORDER BY publish_date DESC";
        String likePattern = "%" + keyword + "%";
        return findBySql(sql, new String[]{likePattern, likePattern, likePattern});
    }

    // Фильтр по категории
    public List<News> findByCategory(String category) {
        String sql = "SELECT * FROM news WHERE category = ? ORDER BY publish_date DESC";
        return findBySql(sql, new String[]{category});
    }

    // Фильтр по источнику
    public List<News> findBySource(String sourceName) {
        String sql = "SELECT * FROM news WHERE source_name = ? ORDER BY publish_date DESC";
        return findBySql(sql, new String[]{sourceName});
    }

    // Фильтр по диапазону дат
    public List<News> findByDateRange(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT * FROM news WHERE publish_date BETWEEN ? AND ? ORDER BY publish_date DESC";
        String startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return findBySql(sql, new String[]{startStr, endStr});
    }

    // Получить все категории
    public List<String> findAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM news WHERE category IS NOT NULL AND category != ''";

        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения категорий: " + e.getMessage());
        }
        return categories;
    }

    // Получить все источники
    public List<String> findAllSources() {
        List<String> sources = new ArrayList<>();
        String sql = "SELECT DISTINCT source_name FROM news WHERE source_name IS NOT NULL";

        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sources.add(rs.getString("source_name"));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения источников: " + e.getMessage());
        }
        return sources;
    }

    // Количество новостей по категориям (для аналитики)
    public int countByCategory(String category) {
        String sql = "SELECT COUNT(*) FROM news WHERE category = ?";
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, category);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка подсчета: " + e.getMessage());
            return 0;
        }
    }

    // Удалить старые новости (например, старше N дней)
    public int deleteOlderThan(int days) {
        String sql = "DELETE FROM news WHERE publish_date < datetime('now', '-' || ? || ' days')";
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, days);
            int deleted = pstmt.executeUpdate();
            System.out.println("Удалено " + deleted + " старых новостей");
            return deleted;
        } catch (SQLException e) {
            System.err.println("Ошибка удаления: " + e.getMessage());
            return 0;
        }
    }

    // Приватный метод для выполнения SQL запросов
    private List<News> findBySql(String sql, String[] params) {
        List<News> newsList = new ArrayList<>();

        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setString(i + 1, params[i]);
                }
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    News news = mapResultSetToNews(rs);
                    if (news != null) {
                        newsList.add(news);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка запроса: " + e.getMessage());
        }

        return newsList;
    }

    // Преобразовать ResultSet в объект News
    private News mapResultSetToNews(ResultSet rs) {
        try {
            News news = new News();
            news.setId(rs.getLong("id"));
            news.setTitle(rs.getString("title"));
            news.setDescription(rs.getString("description"));
            news.setFullText(rs.getString("full_text"));

            // Обработка даты
            String publishDateStr = rs.getString("publish_date");
            if (publishDateStr != null && !publishDateStr.isEmpty()) {
                try {
                    news.setPublishDate(LocalDateTime.parse(publishDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e) {
                    news.setPublishDate(LocalDateTime.now());
                }
            } else {
                news.setPublishDate(LocalDateTime.now());
            }

            news.setSourceUrl(rs.getString("source_url"));
            news.setSourceName(rs.getString("source_name"));
            news.setCategory(rs.getString("category"));
            news.setKeywords(jsonToKeywords(rs.getString("keywords")));
            news.setMediaUrls(jsonToMediaUrls(rs.getString("media_urls")));
            news.setViews(rs.getInt("views"));

            String createdAtStr = rs.getString("created_at");
            if (createdAtStr != null && !createdAtStr.isEmpty()) {
                try {
                    news.setCreatedAt(LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e) {
                    news.setCreatedAt(LocalDateTime.now());
                }
            } else {
                news.setCreatedAt(LocalDateTime.now());
            }

            String updatedAtStr = rs.getString("updated_at");
            if (updatedAtStr != null && !updatedAtStr.isEmpty()) {
                try {
                    news.setUpdatedAt(LocalDateTime.parse(updatedAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e) {
                    news.setUpdatedAt(LocalDateTime.now());
                }
            } else {
                news.setUpdatedAt(LocalDateTime.now());
            }

            return news;
        } catch (SQLException e) {
            System.err.println("Ошибка преобразования ResultSet в News: " + e.getMessage());
            return null;
        }
    }

    // Конвертация списка ключевых слов в JSON
    private String keywordsToJson(List<String> keywords) {
        try {
            return objectMapper.writeValueAsString(keywords);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    // Конвертация JSON в список ключевых слов
    private List<String> jsonToKeywords(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    // Конвертация списка медиа-URL в JSON
    private String mediaUrlsToJson(List<String> mediaUrls) {
        try {
            return objectMapper.writeValueAsString(mediaUrls);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    // Конвертация JSON в список медиа-URL
    private List<String> jsonToMediaUrls(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    // Экспорт в CSV
    public void exportToCsv(String filePath) throws IOException {
        List<News> newsList = findAll();
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("id,title,category,source_name,publish_date,source_url\n");
            for (News news : newsList) {
                writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        news.getId(),
                        escape(news.getTitle()),
                        escape(news.getCategory()),
                        escape(news.getSourceName()),
                        news.getPublishDate() != null ? news.getPublishDate().toString() : "",
                        escape(news.getSourceUrl())));
            }
        }
    }

    // Экспорт в JSON
    public void exportToJson(String filePath) throws IOException {
        List<News> newsList = findAll();
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < newsList.size(); i++) {
            News n = newsList.get(i);
            sb.append("  {\n");
            sb.append(String.format("    \"id\": %d,\n", n.getId()));
            sb.append(String.format("    \"title\": \"%s\",\n", escapeJson(n.getTitle())));
            sb.append(String.format("    \"description\": \"%s\",\n", escapeJson(n.getDescription())));
            sb.append(String.format("    \"category\": \"%s\",\n", escapeJson(n.getCategory())));
            sb.append(String.format("    \"sourceName\": \"%s\",\n", escapeJson(n.getSourceName())));
            sb.append(String.format("    \"sourceUrl\": \"%s\",\n", escapeJson(n.getSourceUrl())));
            sb.append(String.format("    \"publishDate\": \"%s\"\n",
                    n.getPublishDate() != null ? n.getPublishDate().toString() : ""));
            sb.append(i < newsList.size() - 1 ? "  },\n" : "  }\n");
        }
        sb.append("]");
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(sb.toString());
        }
    }

    // Экспорт в HTML
    public void exportToHtml(String filePath) throws IOException {
        List<News> newsList = findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<title>Новости</title></head><body>");
        sb.append("<h1>Новостной агрегатор</h1>");
        for (News news : newsList) {
            sb.append("<div style='border:1px solid #ccc;margin:10px;padding:10px'>");
            sb.append("<h3>").append(escapeHtml(news.getTitle())).append("</h3>");
            sb.append("<p><b>Категория:</b> ").append(escapeHtml(news.getCategory())).append("</p>");
            sb.append("<p><b>Источник:</b> ").append(escapeHtml(news.getSourceName())).append("</p>");
            sb.append("<p><b>Дата:</b> ").append(news.getPublishDate() != null ? news.getPublishDate().toLocalDate() : "").append("</p>");
            if (news.getDescription() != null) {
                sb.append("<p>").append(escapeHtml(news.getDescription())).append("</p>");
            }
            if (news.getSourceUrl() != null) {
                sb.append("<a href='").append(news.getSourceUrl()).append("'>Читать полностью</a>");
            }
            sb.append("</div>");
        }
        sb.append("</body></html>");
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(sb.toString());
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // Запись в историю изменений
    private void logHistory(String sourceUrl, String action) {
        String sql = "INSERT INTO history (news_id, action) SELECT id, ? FROM news WHERE source_url = ?";
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, action);
            pstmt.setString(2, sourceUrl);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка записи истории: " + e.getMessage());
        }
    }
}