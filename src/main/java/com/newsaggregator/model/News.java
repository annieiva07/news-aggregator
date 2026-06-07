package com.newsaggregator.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class News {
    private Long id;
    private String title;
    private String description;
    private String fullText;
    private LocalDateTime publishDate;
    private String sourceUrl;
    private String sourceName;
    private String category;
    private List<String> keywords;
    private List<String> mediaUrls;
    private int views;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Конструктор по умолчанию
    public News() {
        this.keywords = new ArrayList<>();
        this.mediaUrls = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.views = 0;
    }

    // Конструктор с основными полями
    public News(String title, String description, String fullText,
                LocalDateTime publishDate, String sourceUrl, String sourceName) {
        this();
        this.title = title;
        this.description = description;
        this.fullText = fullText;
        this.publishDate = publishDate;
        this.sourceUrl = sourceUrl;
        this.sourceName = sourceName;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public LocalDateTime getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(LocalDateTime publishDate) {
        this.publishDate = publishDate;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public void addKeyword(String keyword) {
        if (!this.keywords.contains(keyword)) {
            this.keywords.add(keyword);
        }
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public void addMediaUrl(String mediaUrl) {
        this.mediaUrls.add(mediaUrl);
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public void incrementViews() {
        this.views++;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("News{id=%d, title='%s', source='%s', publishDate=%s, category='%s'}",
                id,
                title.length() > 50 ? title.substring(0, 47) + "..." : title,
                sourceName,
                publishDate,
                category);
    }

    // Краткое представление для консоли
    public String toShortString(int index) {
        StringBuilder sb = new StringBuilder(String.format(
                "[%d] %s | %s | %s\n    %s\n    Источник: %s",
                index,
                publishDate != null ? publishDate.toLocalDate() : "Дата неизвестна",
                category != null ? category : "Без категории",
                title,
                description != null && description.length() > 80 ?
                        description.substring(0, 77) + "..." : description,
                sourceName));
        if (sourceUrl != null && !sourceUrl.isEmpty() && !sourceUrl.startsWith("manual:")) {
            sb.append("\n    Ссылка: ").append(sourceUrl);
        }
        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            sb.append("\n    Медиа:  ").append(String.join("\n            ", mediaUrls));
        }
        sb.append("\n");
        return sb.toString();
    }

    public String toShortString() {
        return toShortString(id != null ? id.intValue() : 0);
    }
}