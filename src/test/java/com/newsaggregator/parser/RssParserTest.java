package com.newsaggregator.parser;

import com.newsaggregator.model.News;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RssParserTest {

    private final RssParser parser = new RssParser("Тест", "https://example.com/rss");

    private LocalDateTime invokeParseRssDate(String dateStr) throws Exception {
        Method method = RssParser.class.getDeclaredMethod("parseRssDate", String.class);
        method.setAccessible(true);
        return (LocalDateTime) method.invoke(parser, new Object[]{dateStr});
    }

    private String invokeCategory(String title) throws Exception {
        Method method = RssParser.class.getDeclaredMethod("detectCategory", String.class);
        method.setAccessible(true);
        return (String) method.invoke(parser, title);
    }

    // ---------- parseRssDate ----------

    @Test
    void parsesRfc1123GmtDateCorrectly() throws Exception {
        LocalDateTime result = invokeParseRssDate("Sun, 07 Jun 2026 12:00:00 GMT");
        assertEquals(2026, result.getYear());
        assertEquals(6, result.getMonthValue());
        assertEquals(7, result.getDayOfMonth());
        assertEquals(12, result.getHour());
    }

    @Test
    void parsesDateWithPositiveNumericOffset() throws Exception {
        LocalDateTime result = invokeParseRssDate("Thu, 01 Jan 2026 10:30:00 +0300");
        assertEquals(2026, result.getYear());
        assertEquals(1, result.getMonthValue());
    }

    @Test
    void parsesDateWithNegativeNumericOffset() throws Exception {
        LocalDateTime result = invokeParseRssDate("Fri, 15 Mar 2024 08:00:00 -0500");
        assertEquals(2024, result.getYear());
        assertEquals(3, result.getMonthValue());
    }

    @Test
    void parsesIsoDateWithoutTimezone() throws Exception {
        LocalDateTime result = invokeParseRssDate("2026-06-07T10:30:00");
        assertEquals(2026, result.getYear());
        assertEquals(6, result.getMonthValue());
        assertEquals(7, result.getDayOfMonth());
    }

    @Test
    void parsesSimpleDateTimeFormat() throws Exception {
        LocalDateTime result = invokeParseRssDate("2026-06-07 10:30:00");
        assertEquals(2026, result.getYear());
    }

    @Test
    void returnsNowForNullDate() throws Exception {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        LocalDateTime result = invokeParseRssDate(null);
        assertFalse(result.isBefore(before));
    }

    @Test
    void returnsNowForEmptyDate() throws Exception {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        LocalDateTime result = invokeParseRssDate("");
        assertFalse(result.isBefore(before));
    }

    @Test
    void returnsNowForInvalidDate() throws Exception {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        LocalDateTime result = invokeParseRssDate("не-дата-вообще");
        assertFalse(result.isBefore(before));
    }

    // ---------- detectCategory ----------

    @Test
    void detectsCategoryPolitics() throws Exception {
        assertEquals("Политика", invokeCategory("Путин подписал закон"));
    }

    @Test
    void detectsCategoryPoliticsByGovt() throws Exception {
        assertEquals("Политика", invokeCategory("Правительство приняло решение"));
    }

    @Test
    void detectsCategoryPoliticsByDuma() throws Exception {
        assertEquals("Политика", invokeCategory("Госдума рассмотрела вопрос"));
    }

    @Test
    void detectsCategoryPoliticsByElections() throws Exception {
        assertEquals("Политика", invokeCategory("Выборы прошли успешно"));
    }

    @Test
    void detectsCategorySportsByFootball() throws Exception {
        assertEquals("Спорт", invokeCategory("Футбол: финал чемпионата"));
    }

    @Test
    void detectsCategorySportsByHockey() throws Exception {
        assertEquals("Спорт", invokeCategory("Хоккей: Россия победила"));
    }

    @Test
    void detectsCategorySportsBySport() throws Exception {
        assertEquals("Спорт", invokeCategory("Новости спорта за неделю"));
    }

    @Test
    void detectsCategoryEconomics() throws Exception {
        assertEquals("Экономика", invokeCategory("Рубль укрепился к доллару"));
    }

    @Test
    void detectsCategoryEconomicsByBusiness() throws Exception {
        assertEquals("Экономика", invokeCategory("Бизнес ожидает роста"));
    }

    @Test
    void detectsCategoryEconomicsByPrice() throws Exception {
        assertEquals("Экономика", invokeCategory("Цена на нефть выросла"));
    }

    @Test
    void detectsCategoryScience() throws Exception {
        assertEquals("Наука", invokeCategory("Учёные открыли новый элемент"));
    }

    @Test
    void detectsCategoryScienceByResearch() throws Exception {
        assertEquals("Наука", invokeCategory("Исследование показало результаты"));
    }

    @Test
    void detectsCategoryCulture() throws Exception {
        assertEquals("Культура", invokeCategory("Кино: лучшие фильмы года"));
    }

    @Test
    void detectsCategoryCultureByMusic() throws Exception {
        assertEquals("Культура", invokeCategory("Музыка: новый альбом вышел"));
    }

    @Test
    void detectsCategoryOtherByDefault() throws Exception {
        assertEquals("Прочее", invokeCategory("Случайный заголовок новости о погоде"));
    }

    // ---------- getMockNews ----------


    // ---------- getSourceName ----------

    @Test
    void getSourceNameReturnsName() {
        assertEquals("Тест", parser.getSourceName());
    }

    @Test
    void differentParsersHaveDifferentNames() {
        RssParser p2 = new RssParser("Другой", "https://other.com/rss");
        assertNotEquals(parser.getSourceName(), p2.getSourceName());
    }

    // ---------- parseNews ----------

    private Connection buildMockConn(Document doc) throws IOException {
        Connection c = Mockito.mock(Connection.class);
        when(c.userAgent(anyString())).thenReturn(c);
        when(c.header(anyString(), anyString())).thenReturn(c);
        when(c.timeout(anyInt())).thenReturn(c);
        when(c.ignoreContentType(anyBoolean())).thenReturn(c);
        when(c.get()).thenReturn(doc);
        return c;
    }

    @Test
    void parseNewsReturnsMockOnIoException() throws IOException {
        Connection c = Mockito.mock(Connection.class);
        when(c.userAgent(anyString())).thenReturn(c);
        when(c.header(anyString(), anyString())).thenReturn(c);
        when(c.timeout(anyInt())).thenReturn(c);
        when(c.ignoreContentType(anyBoolean())).thenReturn(c);
        when(c.get()).thenThrow(new IOException("test"));
        try (MockedStatic<Jsoup> mocked = Mockito.mockStatic(Jsoup.class)) {
            mocked.when(() -> Jsoup.connect(anyString())).thenReturn(c);
            List<News> result = parser.parseNews();
            assertNotNull(result);
        }
    }

    @Test
    void parseNewsReturnsMockWhenNoItems() throws IOException {
        Document emptyDoc = Jsoup.parse("<rss><channel></channel></rss>", "", Parser.xmlParser());
        Connection mockConn = buildMockConn(emptyDoc);
        try (MockedStatic<Jsoup> mocked = Mockito.mockStatic(Jsoup.class)) {
            mocked.when(() -> Jsoup.connect(anyString())).thenReturn(mockConn);
            List<News> result = parser.parseNews();
            assertNotNull(result);
        }
    }

    @Test
    void parseNewsParsesRssItemsSuccessfully() throws IOException {
        String rss = "<rss><channel>" +
                "<item>" +
                "<title>Путин подписал закон о финансах</title>" +
                "<description>Описание новости</description>" +
                "<pubDate>Sun, 07 Jun 2026 12:00:00 GMT</pubDate>" +
                "<link>https://test.com/news/1</link>" +
                "</item>" +
                "<item><title></title><link>https://test.com/empty</link></item>" +
                "</channel></rss>";
        Document doc = Jsoup.parse(rss, "", Parser.xmlParser());
        Connection mockConn = buildMockConn(doc);
        try (MockedStatic<Jsoup> mocked = Mockito.mockStatic(Jsoup.class)) {
            mocked.when(() -> Jsoup.connect(anyString())).thenReturn(mockConn);
            List<News> result = parser.parseNews();
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Политика", result.get(0).getCategory());
        }
    }
}
