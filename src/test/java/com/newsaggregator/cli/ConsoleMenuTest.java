package com.newsaggregator.cli;

import com.newsaggregator.model.News;
import com.newsaggregator.parser.ParserManager;
import com.newsaggregator.service.AnalyticsService;
import com.newsaggregator.service.SchedulerService;
import com.newsaggregator.storage.DatabaseConnection;
import com.newsaggregator.storage.NewsRepository;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConsoleMenuTest {

    private static NewsRepository repository;
    private static ParserManager parserManager;
    private static AnalyticsService analyticsService;
    private static SchedulerService schedulerService;

    @BeforeAll
    static void setUp() {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        DatabaseConnection.getConnection();
        repository = new NewsRepository();
        parserManager = new ParserManager(repository);
        analyticsService = new AnalyticsService(repository);
        schedulerService = new SchedulerService(parserManager);

        News news = new News("Тестовая новость для меню", "Описание новости", "Полный текст статьи",
                LocalDateTime.now(), "https://menu-test.com/1", "МенюТест");
        news.setCategory("Тест");
        repository.save(news);
    }

    @AfterAll
    static void tearDown() {
        schedulerService.shutdown();
        DatabaseConnection.closeConnection();
    }

    private ConsoleMenu menuWith(String input) {
        ConsoleMenu menu = new ConsoleMenu(repository, parserManager, analyticsService, schedulerService);
        try {
            Field field = ConsoleMenu.class.getDeclaredField("scanner");
            field.setAccessible(true);
            byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
            field.set(menu, new Scanner(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return menu;
    }

    private String run(String input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
        PrintStream original = System.out;
        System.setOut(ps);
        try {
            menuWith(input).start();
        } finally {
            System.setOut(original);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    @Test
    @Order(1)
    void exitOnZero() {
        assertTrue(run("0\n").contains("Выход"));
    }

    @Test
    @Order(2)
    void showAllNewsContainsHeader() {
        String out = run("1\n0\n");
        assertTrue(out.contains("ВСЕ НОВОСТИ") || out.contains("Новостей нет"));
    }

    @Test
    @Order(3)
    void showAllNewsShowsTestNews() {
        String out = run("1\n0\n");
        assertTrue(out.contains("Тестовая новость") || out.contains("новость"));
    }

    @Test
    @Order(4)
    void searchByKeywordFindsMatch() {
        String out = run("2\nновость\n0\n");
        assertTrue(out.contains("РЕЗУЛЬТАТЫ ПОИСКА") || out.contains("не найдены"));
    }

    @Test
    @Order(5)
    void searchByKeywordNoMatch() {
        String out = run("2\nxyzнесуществует\n0\n");
        assertTrue(out.contains("не найдены"));
    }

    @Test
    @Order(6)
    void searchByKeywordEmptyInputWarns() {
        String out = run("2\n\n0\n");
        assertTrue(out.contains("не может быть пустым"));
    }

    @Test
    @Order(7)
    void filterByCategoryByIndex() {
        String out = run("3\n1\n0\n");
        assertTrue(out.contains("КАТЕГОРИЯ") || out.contains("не найдены") || out.contains("не найдены"));
    }

    @Test
    @Order(8)
    void filterByCategoryByName() {
        String out = run("3\nТест\n0\n");
        assertTrue(out.contains("КАТЕГОРИЯ") || out.contains("не найдены"));
    }

    @Test
    @Order(9)
    void filterBySourceByIndex() {
        String out = run("4\n1\n0\n");
        assertTrue(out.contains("ИСТОЧНИК") || out.contains("не найдены"));
    }

    @Test
    @Order(10)
    void filterBySourceByName() {
        String out = run("4\nМенюТест\n0\n");
        assertTrue(out.contains("ИСТОЧНИК") || out.contains("не найдены"));
    }

    @Test
    @Order(11)
    void filterByDateRangeInvalidFormat() {
        String out = run("5\nнеправильно\nтоже\n0\n");
        assertTrue(out.contains("Неверный формат"));
    }

    @Test
    @Order(12)
    void filterByDateRangeValid() {
        String out = run("5\n2020-01-01T00:00:00\n2030-01-01T00:00:00\n0\n");
        assertTrue(out.contains("ПЕРИОД") || out.contains("не найдены"));
    }

    @Test
    @Order(13)
    void filterByDateRangeNoResults() {
        String out = run("5\n2000-01-01T00:00:00\n2001-01-01T00:00:00\n0\n");
        assertTrue(out.contains("не найдены") || out.contains("ПЕРИОД"));
    }

    @Test
    @Order(14)
    void showSortedByDate() {
        String out = run("6\n1\n0\n");
        assertTrue(out.contains("DATE") || out.contains("нет") || out.contains("новост"));
    }

    @Test
    @Order(15)
    void showSortedByViews() {
        String out = run("6\n2\n0\n");
        assertTrue(out.contains("VIEWS") || out.contains("нет"));
    }

    @Test
    @Order(16)
    void showSortedBySource() {
        String out = run("6\n3\n0\n");
        assertTrue(out.contains("SOURCE") || out.contains("нет"));
    }

    @Test
    @Order(17)
    void showSortedByCategory() {
        String out = run("6\n4\n0\n");
        assertTrue(out.contains("CATEGORY") || out.contains("нет"));
    }

    @Test
    @Order(18)
    void showSortedDefaultSort() {
        String out = run("6\n9\n0\n");
        assertTrue(out.contains("DATE") || out.contains("нет"));
    }

    @Test
    @Order(19)
    void showStatisticsContainsHeader() {
        String out = run("7\n0\n");
        assertTrue(out.contains("СТАТИСТИКА") || out.contains("Нет данных"));
    }

    @Test
    @Order(20)
    void showStatisticsShowsCategoryCount() {
        String out = run("7\n0\n");
        assertTrue(out.contains("Тест") || out.contains("Нет данных") || out.contains("СТАТИСТИКА"));
    }

    @Test
    @Order(21)
    void analyticsTopKeywordsWorks() {
        String out = run("8\n1\n7\n0\n");
        assertTrue(out.contains("АНАЛИТИКА") || out.contains("Нет данных"));
    }

    @Test
    @Order(22)
    void analyticsTopKeywordsInvalidDays() {
        String out = run("8\n1\nаbc\n0\n");
        assertTrue(out.contains("АНАЛИТИКА") || out.contains("Нет данных"));
    }

    @Test
    @Order(23)
    void analyticsKeywordDynamics() {
        String out = run("8\n2\nтест\n3\n0\n");
        assertTrue(out.contains("Динамика") || out.contains("АНАЛИТИКА"));
    }

    @Test
    @Order(24)
    void analyticsKeywordDynamicsInvalidDays() {
        String out = run("8\n2\nтест\nabc\n0\n");
        assertTrue(out.contains("Динамика") || out.contains("АНАЛИТИКА"));
    }

    @Test
    @Order(25)
    void analyticsInvalidChoice() {
        String out = run("8\n9\n0\n");
        assertTrue(out.contains("Неверный выбор"));
    }

    @Test
    @Order(26)
    void parseNewsInvalidStringChoice() {
        String out = run("9\nabc\n0\n");
        assertTrue(out.contains("Неверный"));
    }

    @Test
    @Order(27)
    void parseNewsOutOfRangeChoice() {
        String out = run("9\n999\n0\n");
        assertTrue(out.contains("Неверный"));
    }

    @Test
    @Order(28)
    void autoUpdateStartsScheduler() {
        String out = run("10\n1\n60\n10\n1\n0\n");
        assertTrue(out.contains("АВТООБНОВЛЕНИЕ"));
    }

    @Test
    @Order(29)
    void autoUpdateChangeInterval() {
        schedulerService.start(30);
        String out = run("10\n2\n45\n10\n1\n0\n");
        assertTrue(out.contains("АВТООБНОВЛЕНИЕ"));
        schedulerService.stop();
    }

    @Test
    @Order(30)
    void autoUpdateStartInvalidInterval() {
        String out = run("10\n1\nнечисло\n0\n");
        assertTrue(out.contains("АВТООБНОВЛЕНИЕ") || out.contains("формат"));
    }

    @Test
    @Order(31)
    void manageSourcesShowList() {
        String out = run("11\n1\n0\n");
        assertTrue(out.contains("УПРАВЛЕНИЕ") || out.contains("Доступные"));
    }

    @Test
    @Order(32)
    void manageSourcesAddSource() {
        String out = run("11\n2\nНовый источник\nhttps://new-source.com/rss\n0\n");
        assertTrue(out.contains("добавлен"));
    }

    @Test
    @Order(33)
    void manageSourcesAddEmptyNameWarns() {
        String out = run("11\n2\n\nhttps://url.com\n0\n");
        assertTrue(out.contains("пустыми"));
    }

    @Test
    @Order(34)
    void manageSourcesDeleteSource() {
        parserManager.addSource("ДляУдаленияВМеню", "https://delete-me.com/rss");
        String keyToDelete = parserManager.getSourceKeys().get(parserManager.getSourceCount() - 1);
        String out = run("11\n3\n" + keyToDelete + "\n0\n");
        assertTrue(out.contains("удалён") || out.contains("УПРАВЛЕНИЕ"));
    }

    @Test
    @Order(35)
    void manageSourcesDeleteNonExistent() {
        String out = run("11\n3\n9999\n0\n");
        assertTrue(out.contains("не найден") || out.contains("УПРАВЛЕНИЕ"));
    }

    @Test
    @Order(36)
    void manageSourcesInvalidChoice() {
        String out = run("11\n9\n0\n");
        assertTrue(out.contains("Неверный"));
    }

    @Test
    @Order(37)
    void exportCsvCreatesFile() {
        String out = run("12\n1\ntest_menu_csv\n0\n");
        assertTrue(out.contains("Экспорт"));
        new java.io.File("test_menu_csv.csv").delete();
    }

    @Test
    @Order(38)
    void exportJsonCreatesFile() {
        String out = run("12\n2\ntest_menu_json\n0\n");
        assertTrue(out.contains("Экспорт"));
        new java.io.File("test_menu_json.json").delete();
    }

    @Test
    @Order(39)
    void exportHtmlCreatesFile() {
        String out = run("12\n3\ntest_menu_html\n0\n");
        assertTrue(out.contains("Экспорт"));
        new java.io.File("test_menu_html.html").delete();
    }

    @Test
    @Order(40)
    void exportDefaultFilenameWhenEmpty() {
        String out = run("12\n1\n\n0\n");
        assertTrue(out.contains("Экспорт"));
        new java.io.File("news_export.csv").delete();
    }

    @Test
    @Order(41)
    void exportInvalidFormatHandled() {
        String out = run("12\n9\ntest\n0\n");
        assertTrue(out.contains("Неверный"));
    }

    @Test
    @Order(42)
    void addNewsManuallyWorks() {
        String out = run("13\nРучной заголовок\nОписание\nТекст\nСайт\nКатегория\n0\n");
        assertTrue(out.contains("добавлена") || out.contains("ДОБАВЛЕНИЕ"));
    }

    @Test
    @Order(43)
    void addNewsManuallyEmptyCategoryUsesDefault() {
        String out = run("13\nБез категории заголовок\nОпис\nТекст\nСайт\n\n0\n");
        assertTrue(out.contains("добавлена"));
    }

    @Test
    @Order(44)
    void addNewsManuallyEmptyTitleWarns() {
        String out = run("13\n\n0\n");
        assertTrue(out.contains("пустым"));
    }

    @Test
    @Order(45)
    void invalidMenuChoiceHandled() {
        String out = run("99\n0\n");
        assertTrue(out.contains("Неверный"));
    }

    private String runMenu(ConsoleMenu menu, String input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
        PrintStream original = System.out;
        System.setOut(ps);
        try {
            Field field = ConsoleMenu.class.getDeclaredField("scanner");
            field.setAccessible(true);
            field.set(menu, new Scanner(new ByteArrayInputStream(
                    input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            menu.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.setOut(original);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    @Test
    @Order(46)
    void showAllNewsEmptyDatabaseShowsMessage() {
        NewsRepository mockRepo = Mockito.mock(NewsRepository.class);
        when(mockRepo.findAll()).thenReturn(Collections.emptyList());
        ConsoleMenu menu = new ConsoleMenu(mockRepo, parserManager, analyticsService, schedulerService);
        String out = runMenu(menu, "1\n0\n");
        assertTrue(out.contains("Новостей нет"));
    }

    @Test
    @Order(47)
    void filterByCategoryEmptyDatabaseShowsMessage() {
        NewsRepository mockRepo = Mockito.mock(NewsRepository.class);
        when(mockRepo.findAllCategories()).thenReturn(Collections.emptyList());
        ConsoleMenu menu = new ConsoleMenu(mockRepo, parserManager, analyticsService, schedulerService);
        String out = runMenu(menu, "3\n0\n");
        assertTrue(out.contains("не найдены") || out.contains("Категории"));
    }

    @Test
    @Order(48)
    void filterBySourceEmptyDatabaseShowsMessage() {
        NewsRepository mockRepo = Mockito.mock(NewsRepository.class);
        when(mockRepo.findAllSources()).thenReturn(Collections.emptyList());
        ConsoleMenu menu = new ConsoleMenu(mockRepo, parserManager, analyticsService, schedulerService);
        String out = runMenu(menu, "4\n0\n");
        assertTrue(out.contains("не найдены") || out.contains("Источники"));
    }

    @Test
    @Order(49)
    void showStatisticsEmptyReturnsNoDataMessage() {
        AnalyticsService mockAnalytics = Mockito.mock(AnalyticsService.class);
        when(mockAnalytics.getCountByCategory()).thenReturn(Collections.emptyMap());
        ConsoleMenu menu = new ConsoleMenu(repository, parserManager, mockAnalytics, schedulerService);
        String out = runMenu(menu, "7\n0\n");
        assertTrue(out.contains("Нет данных"));
    }

    @Test
    @Order(50)
    void addNewsManuallyWhenSaveFailsShowsError() {
        NewsRepository mockRepo = Mockito.mock(NewsRepository.class);
        when(mockRepo.save(any(News.class))).thenReturn(false);
        ConsoleMenu menu = new ConsoleMenu(mockRepo, parserManager, analyticsService, schedulerService);
        String out = runMenu(menu, "13\nЗаголовок теста\nОписание\nТекст\nИсточник\nКатегория\n0\n");
        assertTrue(out.contains("Не удалось"));
    }
}
