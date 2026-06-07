package com.newsaggregator.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewsTest {

    @Test
    void defaultConstructorInitializesLists() {
        News news = new News();
        assertNotNull(news.getKeywords());
        assertNotNull(news.getMediaUrls());
        assertTrue(news.getKeywords().isEmpty());
        assertTrue(news.getMediaUrls().isEmpty());
        assertEquals(0, news.getViews());
    }

    @Test
    void defaultConstructorSetsTimestamps() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        News news = new News();
        assertNotNull(news.getCreatedAt());
        assertNotNull(news.getUpdatedAt());
        assertFalse(news.getCreatedAt().isBefore(before));
    }

    @Test
    void parametrizedConstructorSetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        News news = new News("Заголовок", "Описание", "Полный текст", now,
                "https://example.com", "Источник");

        assertEquals("Заголовок", news.getTitle());
        assertEquals("Описание", news.getDescription());
        assertEquals("Полный текст", news.getFullText());
        assertEquals(now, news.getPublishDate());
        assertEquals("https://example.com", news.getSourceUrl());
        assertEquals("Источник", news.getSourceName());
    }

    @Test
    void addKeywordIgnoresDuplicates() {
        News news = new News();
        news.addKeyword("политика");
        news.addKeyword("политика");
        assertEquals(1, news.getKeywords().size());
    }

    @Test
    void addKeywordAcceptsMultipleUnique() {
        News news = new News();
        news.addKeyword("политика");
        news.addKeyword("экономика");
        assertEquals(2, news.getKeywords().size());
        assertTrue(news.getKeywords().contains("политика"));
        assertTrue(news.getKeywords().contains("экономика"));
    }

    @Test
    void addMediaUrlAddsToList() {
        News news = new News();
        news.addMediaUrl("https://img.example.com/1.jpg");
        news.addMediaUrl("https://img.example.com/2.jpg");
        assertEquals(2, news.getMediaUrls().size());
    }

    @Test
    void incrementViewsIncrementsCorrectly() {
        News news = new News();
        news.incrementViews();
        news.incrementViews();
        assertEquals(2, news.getViews());
    }

    @Test
    void settersAndGetters() {
        News news = new News();
        news.setId(42L);
        news.setCategory("Спорт");
        news.setViews(100);
        news.setTitle("Тест");
        news.setDescription("Описание");
        news.setFullText("Текст");
        news.setSourceUrl("https://test.com");
        news.setSourceName("Сайт");
        news.setKeywords(List.of("ключ1", "ключ2"));
        news.setMediaUrls(List.of("https://img.com/a.jpg"));

        assertEquals(42L, news.getId());
        assertEquals("Спорт", news.getCategory());
        assertEquals(100, news.getViews());
        assertEquals("Тест", news.getTitle());
        assertEquals(2, news.getKeywords().size());
        assertEquals(1, news.getMediaUrls().size());
    }

    @Test
    void toShortStringContainsTitleAndCategory() {
        News news = new News("Тестовый заголовок", "Описание", "Текст",
                LocalDateTime.now(), "https://test.com", "Тест");
        news.setId(1L);
        news.setCategory("Политика");
        String result = news.toShortString();
        assertTrue(result.contains("Тестовый заголовок"));
        assertTrue(result.contains("Политика"));
    }

    @Test
    void toShortStringHandlesNullPublishDate() {
        News news = new News();
        news.setId(1L);
        news.setTitle("Заголовок");
        assertDoesNotThrow(() -> news.toShortString());
        assertTrue(news.toShortString().contains("Дата неизвестна"));
    }

    @Test
    void toShortStringTruncatesLongDescription() {
        String longDesc = "А".repeat(200);
        News news = new News("Заголовок", longDesc, "Текст",
                LocalDateTime.now(), "https://test.com", "Тест");
        news.setId(1L);
        String result = news.toShortString();
        assertFalse(result.contains(longDesc));
        assertTrue(result.contains("..."));
    }

    @Test
    void toStringContainsTitleAndSource() {
        News news = new News("Заголовок", "Описание", "Текст",
                LocalDateTime.now(), "https://test.com", "МойСайт");
        news.setId(5L);
        news.setCategory("Наука");
        String result = news.toString();
        assertTrue(result.contains("МойСайт"));
        assertTrue(result.contains("Наука"));
    }

    @Test
    void toStringTruncatesLongTitle() {
        String longTitle = "Б".repeat(100);
        News news = new News(longTitle, "Описание", "Текст",
                LocalDateTime.now(), "https://test.com", "Тест");
        news.setId(1L);
        String result = news.toString();
        assertTrue(result.contains("..."));
    }

    @Test
    void setPublishDateNull() {
        News news = new News();
        news.setPublishDate(null);
        assertNull(news.getPublishDate());
    }

    @Test
    void setCreatedAtAndUpdatedAt() {
        News news = new News();
        LocalDateTime dt = LocalDateTime.of(2026, 1, 1, 0, 0);
        news.setCreatedAt(dt);
        news.setUpdatedAt(dt);
        assertEquals(dt, news.getCreatedAt());
        assertEquals(dt, news.getUpdatedAt());
    }
}
