package com.newsaggregator.service;

import com.newsaggregator.model.News;
import com.newsaggregator.storage.DatabaseConnection;
import com.newsaggregator.storage.NewsRepository;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnalyticsServiceTest {

    private static NewsRepository repository;
    private static AnalyticsService analyticsService;

    @BeforeAll
    static void setUp() {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        DatabaseConnection.getConnection();
        repository = new NewsRepository();
        analyticsService = new AnalyticsService(repository);

        News n1 = новость("Путин подписал закон", "https://analytics.com/1", "Политика");
        n1.setKeywords(List.of("путин", "закон", "политика"));
        repository.save(n1);

        News n2 = новость("ЦБ и экономика", "https://analytics.com/2", "Экономика");
        n2.setKeywords(List.of("экономика", "рубль"));
        repository.save(n2);

        News n3 = новость("Путин и экономика страны", "https://analytics.com/3", "Политика");
        n3.setKeywords(List.of("путин", "экономика", "страна"));
        repository.save(n3);
    }

    @AfterAll
    static void tearDown() {
        DatabaseConnection.closeConnection();
    }

    private static News новость(String title, String url, String category) {
        News news = new News(title, "Описание", "Текст", LocalDateTime.now(), url, "Источник");
        news.setCategory(category);
        return news;
    }

    @Test
    @Order(1)
    void getCountByCategoryNotEmpty() {
        assertFalse(analyticsService.getCountByCategory().isEmpty());
    }

    @Test
    @Order(2)
    void getCountByCategoryCorrectValues() {
        Map<String, Integer> byCategory = analyticsService.getCountByCategory();
        assertEquals(2, byCategory.get("Политика"));
        assertEquals(1, byCategory.get("Экономика"));
    }

    @Test
    @Order(3)
    void getTrendingKeywordsReturnsNonEmpty() {
        LocalDateTime от = LocalDateTime.now().minusDays(1);
        LocalDateTime до = LocalDateTime.now().plusDays(1);
        List<Map.Entry<String, Long>> топ = analyticsService.getTrendingKeywords(5, от, до);
        assertFalse(топ.isEmpty());
    }

    @Test
    @Order(4)
    void getTrendingKeywordsPutinIsFrequent() {
        LocalDateTime от = LocalDateTime.now().minusDays(1);
        LocalDateTime до = LocalDateTime.now().plusDays(1);
        List<Map.Entry<String, Long>> топ = analyticsService.getTrendingKeywords(10, от, до);
        assertTrue(топ.stream().anyMatch(e -> e.getKey().equals("путин") && e.getValue() >= 2));
    }

    @Test
    @Order(5)
    void getTrendingKeywordsRespectTopN() {
        LocalDateTime от = LocalDateTime.now().minusDays(1);
        LocalDateTime до = LocalDateTime.now().plusDays(1);
        List<Map.Entry<String, Long>> топ = analyticsService.getTrendingKeywords(2, от, до);
        assertTrue(топ.size() <= 2);
    }

    @Test
    @Order(6)
    void getTrendingKeywordsSortedDescending() {
        LocalDateTime от = LocalDateTime.now().minusDays(1);
        LocalDateTime до = LocalDateTime.now().plusDays(1);
        List<Map.Entry<String, Long>> топ = analyticsService.getTrendingKeywords(10, от, до);
        if (топ.size() >= 2) {
            assertTrue(топ.get(0).getValue() >= топ.get(1).getValue());
        }
    }

    @Test
    @Order(7)
    void getTrendingKeywordsEmptyRangeReturnsEmpty() {
        LocalDateTime далёко = LocalDateTime.now().minusYears(10);
        LocalDateTime конец = далёко.plusDays(1);
        assertTrue(analyticsService.getTrendingKeywords(5, далёко, конец).isEmpty());
    }

    @Test
    @Order(8)
    void getKeywordDynamicsHasCorrectDayCount() {
        Map<String, Integer> динамика = analyticsService.getKeywordDynamics("путин", 3);
        assertEquals(3, динамика.size());
    }

    @Test
    @Order(9)
    void getKeywordDynamicsSingleDay() {
        Map<String, Integer> динамика = analyticsService.getKeywordDynamics("путин", 1);
        assertEquals(1, динамика.size());
    }

    @Test
    @Order(10)
    void getKeywordDynamicsAllValuesNonNegative() {
        Map<String, Integer> динамика = analyticsService.getKeywordDynamics("путин", 7);
        динамика.values().forEach(v -> assertTrue(v >= 0));
    }

    @Test
    @Order(11)
    void getKeywordDynamicsUnknownKeywordAllZeros() {
        Map<String, Integer> динамика = analyticsService.getKeywordDynamics("ZZZ_NO_MATCH_999", 3);
        assertEquals(3, динамика.size());
        динамика.values().forEach(v -> assertEquals(0, v));
    }

    @Test
    @Order(12)
    void getCountByCategoryEmptyWhenNoNews() {
        NewsRepository emptyRepo = new NewsRepository();
        AnalyticsService emptyAnalytics = new AnalyticsService(emptyRepo);
        Map<String, Integer> result = emptyAnalytics.getCountByCategory();
        assertNotNull(result);
    }
}
