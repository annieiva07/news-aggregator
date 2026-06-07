package com.newsaggregator.parser;

import com.newsaggregator.model.News;
import com.newsaggregator.service.KeywordExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RssParser implements NewsParser {
    private final String name;
    private final String rssUrl;
    private final KeywordExtractor keywordExtractor = new KeywordExtractor();
    private static final Pattern IMG_SRC_PATTERN =
            Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    );
    public RssParser(String name, String rssUrl) {
        this.name = name;
        this.rssUrl = rssUrl;
    }

    @Override
    public String getSourceName() {
        return name;
    }

    @Override
    public List<News> parseNews() {
        List<News> newsList = new ArrayList<>();

        try {
            System.out.println("Парсинг RSS с " + rssUrl + "...");

            Document doc = Jsoup.connect(rssUrl)
                    .userAgent(getRandomUserAgent())
                    .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                    .timeout(15000)
                    .ignoreContentType(true)
                    .get();

            Elements items = doc.select("item");

            if (items.isEmpty()) {
                System.out.println("RSS не содержит новостей.");
                return getMockNews();
            }

            int count = 0;
            for (Element item : items) {
                if (count >= 15) break;

                String title = getElementText(item, "title");
                String description = getElementText(item, "description");
                String pubDateStr = getElementText(item, "pubDate");
                String link = getElementText(item, "link");

                if (title.isEmpty()) continue;

                LocalDateTime publishDate = parseRssDate(pubDateStr);
                String category = detectCategory(title);

                News news = new News(
                        title,
                        description.length() > 300 ? description.substring(0, 297) + "..." : description,
                        description,
                        publishDate,
                        link,
                        name
                );
                news.setCategory(category);
                news.setKeywords(keywordExtractor.extract(title, description));
                news.setMediaUrls(extractMediaUrls(item));
                newsList.add(news);
                count++;
            }

            System.out.println("Собрано " + newsList.size() + " новостей с " + name);

        } catch (IOException e) {
            System.err.println("Ошибка подключения к RSS " + name + ": " + e.getMessage());
            return getMockNews();
        }

        return newsList;
    }

    private String getRandomUserAgent() {
        return USER_AGENTS.get(new java.util.Random().nextInt(USER_AGENTS.size()));
    }

    private List<String> extractMediaUrls(Element item) {
        List<String> urls = new ArrayList<>();

        // <enclosure url="..."/> — стандарт RSS 2.0
        for (Element enclosure : item.getElementsByTag("enclosure")) {
            String url = enclosure.attr("url");
            if (!url.isEmpty()) urls.add(url);
        }

        // <media:content url="..."/> — Media RSS
        for (Element media : item.getElementsByTag("media:content")) {
            String url = media.attr("url");
            if (!url.isEmpty() && !urls.contains(url)) urls.add(url);
        }

        // <media:thumbnail url="..."/> — превью Media RSS
        for (Element thumb : item.getElementsByTag("media:thumbnail")) {
            String url = thumb.attr("url");
            if (!url.isEmpty() && !urls.contains(url)) urls.add(url);
        }

        // Изображения внутри HTML в <description> (характерно для российских RSS)
        if (urls.isEmpty()) {
            Element descEl = item.selectFirst("description");
            if (descEl != null) {
                Matcher m = IMG_SRC_PATTERN.matcher(descEl.text());
                while (m.find()) {
                    String src = m.group(1);
                    if (!src.isEmpty() && !urls.contains(src)) urls.add(src);
                }
            }
        }

        return urls;
    }

    private String getElementText(Element parent, String tagName) {
        Element element = parent.selectFirst(tagName);
        return element != null ? element.text().trim() : "";
    }

    private LocalDateTime parseRssDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            return ZonedDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toLocalDateTime();
        } catch (Exception ignored) {}

        String[] zonedPatterns = {
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                "dd MMM yyyy HH:mm:ss Z",
                "E, d MMM yyyy HH:mm:ss Z",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
        };

        for (String pattern : zonedPatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                return ZonedDateTime.parse(dateStr, formatter).toLocalDateTime();
            } catch (Exception ignored) {}
        }

        String[] localPatterns = {
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : localPatterns) {
            try {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
            } catch (Exception ignored) {}
        }

        return LocalDateTime.now();
    }

    private List<News> getMockNews() {
        List<News> mockList = new ArrayList<>();
        String[][] mockData = {
                {"Путин подписал закон о цифровых финансах", "Политика"},
                {"ЦБ улучшил прогноз по ВВП на 2026 год", "Экономика"},
                {"Российские учёные создали новый материал", "Наука"},
                {"Сборная России выиграла чемпионат по хоккею", "Спорт"},
                {"В Москве открылся новый театральный фестиваль", "Культура"},
                {"Курс рубля укрепился по отношению к доллару", "Экономика"},
                {"В Госдуму внесли законопроект о поддержке IT", "Политика"},
                {"Учёные нашли новый способ лечения аллергии", "Наука"}
        };
        for (int i = 0; i < mockData.length; i++) {
            News news = new News(
                    mockData[i][0],
                    "Демонстрационная новость для " + name,
                    "Полный текст новости: " + mockData[i][0],
                    LocalDateTime.now(),
                    "https://example.com/demo/" + i,
                    name
            );
            news.setCategory(mockData[i][1]);
            mockList.add(news);
        }
        return mockList;
    }

    private String detectCategory(String title) {
        String lowerTitle = title.toLowerCase();

        if (lowerTitle.contains("путин") || lowerTitle.contains("правительство") ||
                lowerTitle.contains("закон") || lowerTitle.contains("выборы") ||
                lowerTitle.contains("госдума") || lowerTitle.contains("политика")) {
            return "Политика";
        } else if (lowerTitle.contains("футбол") || lowerTitle.contains("хоккей") ||
                lowerTitle.contains("спорт") || lowerTitle.contains("чемпионат")) {
            return "Спорт";
        } else if (lowerTitle.contains("рубль") || lowerTitle.contains("цена") ||
                lowerTitle.contains("экономика") || lowerTitle.contains("бизнес") ||
                lowerTitle.contains("акции")) {
            return "Экономика";
        } else if (lowerTitle.contains("наука") || lowerTitle.contains("учёные") ||
                lowerTitle.contains("исследование")) {
            return "Наука";
        } else if (lowerTitle.contains("кино") || lowerTitle.contains("музыка") ||
                lowerTitle.contains("артист")) {
            return "Культура";
        } else {
            return "Прочее";
        }
    }

}