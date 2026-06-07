package com.newsaggregator.storage;

import com.newsaggregator.model.News;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NewsRepositoryTest {

    private static NewsRepository repository;

    @BeforeAll
    static void setUp() {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        DatabaseConnection.getConnection();
        repository = new NewsRepository();
    }

    @AfterAll
    static void tearDown() {
        DatabaseConnection.closeConnection();
    }

    private News makeNews(String title, String url) {
        News news = new News(title, "Описание", "Полный текст",
                LocalDateTime.now(), url, "Тестовый источник");
        news.setCategory("Тест");
        return news;
    }

    @Test
    @Order(1)
    void saveNewNewsReturnsTrue() {
        assertTrue(repository.save(makeNews("Новость 1", "https://test.com/1")));
    }

    @Test
    @Order(2)
    void saveDuplicateUrlReturnsFalse() {
        repository.save(makeNews("Оригинал", "https://test.com/dup"));
        assertFalse(repository.save(makeNews("Дубликат", "https://test.com/dup")));
    }

    @Test
    @Order(3)
    void saveNewsWithNullPublishDate() {
        News news = new News("Без даты", "Описание", "Текст",
                null, "https://test.com/nodate", "Источник");
        assertDoesNotThrow(() -> repository.save(news));
    }

    @Test
    @Order(4)
    void saveNewsWithNullCreatedAt() {
        News news = makeNews("Без createdAt", "https://test.com/nocreated");
        news.setCreatedAt(null);
        news.setUpdatedAt(null);
        assertDoesNotThrow(() -> repository.save(news));
    }

    @Test
    @Order(5)
    void findAllReturnsSavedNews() {
        List<News> all = repository.findAll();
        assertFalse(all.isEmpty());
    }

    @Test
    @Order(6)
    void findAllOrderedByDateAscending() {
        List<News> all = repository.findAll();
        if (all.size() >= 2) {
            LocalDateTime first = all.get(0).getPublishDate();
            LocalDateTime last = all.get(all.size() - 1).getPublishDate();
            if (first != null && last != null) {
                assertFalse(first.isAfter(last));
            }
        }
    }

    @Test
    @Order(7)
    void searchByKeywordFindsMatch() {
        repository.save(makeNews("Уникальная строка xyz987", "https://test.com/search"));
        List<News> results = repository.searchByKeyword("xyz987");
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getTitle().contains("xyz987"));
    }

    @Test
    @Order(8)
    void searchByKeywordReturnsEmptyForNoMatch() {
        assertTrue(repository.searchByKeyword("ZZZ_NOT_EXIST_ZZZ").isEmpty());
    }

    @Test
    @Order(9)
    void findByCategoryReturnsCorrect() {
        News news = makeNews("Категория тест", "https://test.com/cat");
        news.setCategory("УникальнаяКатегория_АБВ");
        repository.save(news);
        List<News> results = repository.findByCategory("УникальнаяКатегория_АБВ");
        assertFalse(results.isEmpty());
        assertEquals("УникальнаяКатегория_АБВ", results.get(0).getCategory());
    }

    @Test
    @Order(10)
    void findBySourceReturnsCorrect() {
        News news = new News("Новость источника", "Опис", "Текст",
                LocalDateTime.now(), "https://test.com/src", "УникальныйИсточник_АБВ");
        repository.save(news);
        List<News> results = repository.findBySource("УникальныйИсточник_АБВ");
        assertFalse(results.isEmpty());
        assertEquals("УникальныйИсточник_АБВ", results.get(0).getSourceName());
    }

    @Test
    @Order(11)
    void findAllCategoriesNotNull() {
        assertNotNull(repository.findAllCategories());
        assertFalse(repository.findAllCategories().isEmpty());
    }

    @Test
    @Order(12)
    void findAllSourcesNotNull() {
        assertNotNull(repository.findAllSources());
        assertFalse(repository.findAllSources().isEmpty());
    }

    @Test
    @Order(13)
    void countByCategoryReturnsPositive() {
        News news = makeNews("Для подсчёта", "https://test.com/count");
        news.setCategory("СчётКатегория_АБВ");
        repository.save(news);
        assertEquals(1, repository.countByCategory("СчётКатегория_АБВ"));
    }

    @Test
    @Order(14)
    void countByCategoryUnknownReturnsZero() {
        assertEquals(0, repository.countByCategory("НесуществующаяXYZ999"));
    }

    @Test
    @Order(15)
    void findByDateRangeIncludesRecentNews() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<News> results = repository.findByDateRange(start, end);
        assertFalse(results.isEmpty());
    }

    @Test
    @Order(16)
    void findByDateRangeExcludesOldNews() {
        LocalDateTime start = LocalDateTime.now().minusYears(10);
        LocalDateTime end = LocalDateTime.now().minusYears(9);
        List<News> results = repository.findByDateRange(start, end);
        assertTrue(results.isEmpty());
    }

    @Test
    @Order(17)
    void findAllSortedByDateWorks() {
        List<News> sorted = repository.findAllSortedBy("date");
        assertFalse(sorted.isEmpty());
    }

    @Test
    @Order(18)
    void findAllSortedByViewsWorks() {
        assertFalse(repository.findAllSortedBy("views").isEmpty());
    }

    @Test
    @Order(19)
    void findAllSortedBySourceWorks() {
        assertFalse(repository.findAllSortedBy("source").isEmpty());
    }

    @Test
    @Order(20)
    void findAllSortedByCategoryWorks() {
        assertFalse(repository.findAllSortedBy("category").isEmpty());
    }

    @Test
    @Order(21)
    void findAllSortedByUnknownFieldFallsBackToDate() {
        assertFalse(repository.findAllSortedBy("unknown").isEmpty());
    }

    @Test
    @Order(22)
    void exportToCsvCreatesFile() throws IOException {
        repository.exportToCsv("test_repo.csv");
        File file = new File("test_repo.csv");
        try {
            assertTrue(file.exists());
            assertTrue(file.length() > 0);
        } finally {
            file.delete();
        }
    }

    @Test
    @Order(23)
    void exportToJsonCreatesFile() throws IOException {
        repository.exportToJson("test_repo.json");
        File file = new File("test_repo.json");
        try {
            assertTrue(file.exists());
            assertTrue(file.length() > 0);
        } finally {
            file.delete();
        }
    }

    @Test
    @Order(24)
    void exportToHtmlCreatesFile() throws IOException {
        repository.exportToHtml("test_repo.html");
        File file = new File("test_repo.html");
        try {
            assertTrue(file.exists());
            assertTrue(file.length() > 0);
        } finally {
            file.delete();
        }
    }

    @Test
    @Order(25)
    void deleteOlderThanDoesNotDeleteRecent() {
        int deleted = repository.deleteOlderThan(3650);
        assertEquals(0, deleted);
    }

    @Test
    @Order(26)
    void mapResultSetRestoresKeywords() {
        News news = makeNews("С ключевыми словами", "https://test.com/kw");
        news.setKeywords(List.of("ключ1", "ключ2", "ключ3"));
        repository.save(news);

        List<News> results = repository.searchByKeyword("ключевыми");
        assertFalse(results.isEmpty());
        assertFalse(results.get(0).getKeywords().isEmpty());
    }

    @Test
    @Order(27)
    void mapResultSetRestoresMediaUrls() {
        News news = makeNews("С медиа", "https://test.com/media");
        news.addMediaUrl("https://img.com/photo.jpg");
        repository.save(news);

        List<News> results = repository.searchByKeyword("медиа");
        assertFalse(results.isEmpty());
        assertFalse(results.get(0).getMediaUrls().isEmpty());
    }
}
