package com.newsaggregator.parser;

import com.newsaggregator.model.News;
import com.newsaggregator.storage.DatabaseConnection;
import com.newsaggregator.storage.NewsRepository;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ParserManagerTest {

    private static NewsRepository mockRepository;
    private ParserManager parserManager;

    @BeforeAll
    static void initDb() {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        DatabaseConnection.getConnection();
        mockRepository = Mockito.mock(NewsRepository.class);
        when(mockRepository.save(any(News.class))).thenReturn(true);
    }

    @AfterAll
    static void tearDown() {
        DatabaseConnection.closeConnection();
    }

    @BeforeEach
    void setUp() {
        parserManager = new ParserManager(mockRepository);
    }

    private News сделатьНовость(String url) {
        return new News("Заголовок", "Описание", "Текст",
                LocalDateTime.now(), url, "Источник");
    }

    private NewsParser mockParser(String name, String... urls) {
        NewsParser mp = Mockito.mock(NewsParser.class);
        when(mp.getSourceName()).thenReturn(name);
        List<News> новости = new java.util.ArrayList<>();
        for (String url : urls) {
            новости.add(сделатьНовость(url));
        }
        when(mp.parseNews()).thenReturn(новости);
        return mp;
    }

    @Test
    @Order(1)
    void initialSourceCountIsThree() {
        assertEquals(3, parserManager.getSourceCount());
    }

    @Test
    @Order(2)
    void addSourceIncreasesCount() {
        parserManager.addSource("Тест", "https://test.com/rss");
        assertEquals(4, parserManager.getSourceCount());
    }

    @Test
    @Order(3)
    void addParserIncreasesCount() {
        NewsParser mp = mockParser("МокИсточник", "https://mock.com/1");
        parserManager.addParser(mp);
        assertEquals(4, parserManager.getSourceCount());
    }

    @Test
    @Order(4)
    void removeSourceDecreasesCount() {
        parserManager.addSource("ДляУдаления", "https://del.com/rss");
        int countBefore = parserManager.getSourceCount();
        String key = parserManager.getSourceKeys().get(countBefore - 1);
        assertTrue(parserManager.removeSource(key));
        assertEquals(countBefore - 1, parserManager.getSourceCount());
    }

    @Test
    @Order(5)
    void removeNonExistentSourceReturnsFalse() {
        assertFalse(parserManager.removeSource("999"));
    }

    @Test
    @Order(6)
    void parseBySourceWithMockParserCallsSave() {
        Mockito.reset(mockRepository);
        when(mockRepository.save(any(News.class))).thenReturn(true);

        NewsParser mp = mockParser("МокИсточник", "https://only-mock.com/1", "https://only-mock.com/2");

        ParserManager pm = new ParserManager(mockRepository);
        pm.addParser(mp);

        String specificKey = pm.getSourceKeys().get(pm.getSourceCount() - 1);
        int saved = pm.parseBySource(specificKey);

        assertEquals(2, saved);
        verify(mp, times(1)).parseNews();
        verify(mockRepository, times(2)).save(any(News.class));
    }

    @Test
    @Order(7)
    void parseBySourceAllParsesEveryone() {
        Mockito.reset(mockRepository);
        when(mockRepository.save(any(News.class))).thenReturn(true);

        NewsParser mp1 = mockParser("М1", "https://m1.com/1");
        NewsParser mp2 = mockParser("М2", "https://m2.com/1", "https://m2.com/2");

        ParserManager pm = new ParserManager(mockRepository);
        pm.removeSource("1");
        pm.removeSource("1");
        pm.removeSource("1");
        pm.addParser(mp1);
        pm.addParser(mp2);

        String allKey = String.valueOf(pm.getSourceCount() + 1);
        int saved = pm.parseBySource(allKey);

        verify(mp1, times(1)).parseNews();
        verify(mp2, times(1)).parseNews();
        assertEquals(3, saved);
    }

    @Test
    @Order(8)
    void parseBySourceInvalidChoiceReturnsZero() {
        int result = parserManager.parseBySource("999");
        assertEquals(0, result);
    }

    @Test
    @Order(9)
    void parseBySourceSaveReturnsFalseNotCounted() {
        Mockito.reset(mockRepository);
        when(mockRepository.save(any(News.class))).thenReturn(false);

        NewsParser mp = mockParser("МокДубль", "https://dup.com/1");
        ParserManager pm = new ParserManager(mockRepository);
        pm.addParser(mp);
        String key = pm.getSourceKeys().get(pm.getSourceCount() - 1);

        int saved = pm.parseBySource(key);
        assertEquals(0, saved);
    }

    @Test
    @Order(10)
    void showAvailableSourcesDoesNotThrow() {
        assertDoesNotThrow(() -> parserManager.showAvailableSources());
    }

    @Test
    @Order(11)
    void getSourceKeysNotEmpty() {
        assertFalse(parserManager.getSourceKeys().isEmpty());
    }

    @Test
    @Order(12)
    void parseAndSaveDelegatesToAllSources() {
        Mockito.reset(mockRepository);
        when(mockRepository.save(any(News.class))).thenReturn(true);

        NewsParser mp = mockParser("МокАnd", "https://and.com/1");
        ParserManager pm = new ParserManager(mockRepository);
        pm.addParser(mp);

        assertDoesNotThrow(pm::parseAndSave);
        verify(mp, times(1)).parseNews();
    }
}
