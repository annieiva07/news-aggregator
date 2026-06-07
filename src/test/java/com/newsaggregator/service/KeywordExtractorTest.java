package com.newsaggregator.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeywordExtractorTest {

    private final KeywordExtractor extractor = new KeywordExtractor();

    @Test
    void extractsWordsLongerThanThree() {
        List<String> keywords = extractor.extract("путин подписал закон", "");
        assertFalse(keywords.isEmpty());
    }

    @Test
    void ignoresStopWords() {
        List<String> keywords = extractor.extract("и в на с по для от до", "");
        assertTrue(keywords.isEmpty());
    }

    @Test
    void ignoresShortWords() {
        List<String> keywords = extractor.extract("аб вгд", "");
        assertTrue(keywords.isEmpty());
    }

    @Test
    void returnsAtMostFiveKeywords() {
        String text = "путин правительство госдума экономика финансы рубль бизнес спорт наука культура";
        List<String> keywords = extractor.extract(text, text);
        assertTrue(keywords.size() <= 5);
    }

    @Test
    void handlesNullTitle() {
        assertDoesNotThrow(() -> extractor.extract(null, "описание текста"));
        assertFalse(extractor.extract(null, "описание текста").isEmpty());
    }

    @Test
    void handlesNullDescription() {
        assertDoesNotThrow(() -> extractor.extract("заголовок текст", null));
    }

    @Test
    void handlesBothNull() {
        List<String> keywords = extractor.extract(null, null);
        assertNotNull(keywords);
        assertTrue(keywords.isEmpty());
    }

    @Test
    void handlesBothEmpty() {
        List<String> keywords = extractor.extract("", "");
        assertNotNull(keywords);
    }

    @Test
    void combinesTitleAndDescription() {
        List<String> keywords = extractor.extract("путин", "правительство подписало закон");
        assertFalse(keywords.isEmpty());
    }

    @Test
    void mostFrequentWordIsFirst() {
        String text = "политика политика политика экономика экономика спорт";
        List<String> keywords = extractor.extract(text, "");
        assertFalse(keywords.isEmpty());
        assertEquals("политика", keywords.get(0));
    }

    @Test
    void wordsMustBeAtLeastFourChars() {
        List<String> keywords = extractor.extract("это всё нет", "");
        assertTrue(keywords.isEmpty());
    }

    @Test
    void deduplicatesWords() {
        String text = "экономика экономика экономика";
        List<String> keywords = extractor.extract(text, "");
        assertEquals(1, keywords.size());
    }
}
