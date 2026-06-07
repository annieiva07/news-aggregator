package com.newsaggregator.parser;

import com.newsaggregator.model.News;
import com.newsaggregator.storage.NewsRepository;

import java.util.ArrayList;
import java.util.List;

public class ParserManager {

    private final List<NewsParser> parsers;
    private final NewsRepository repository;

    public ParserManager(NewsRepository repository) {
        this.repository = repository;
        this.parsers = new ArrayList<>();

        parsers.add(new RssParser("ТАСС", "https://tass.ru/rss/v2.xml"));
        parsers.add(new RssParser("Интерфакс", "https://www.interfax.ru/rss.asp"));
    }

    public void addSource(String name, String rssUrl) {
        parsers.add(new RssParser(name, rssUrl));
        System.out.println("Источник добавлен: " + name);
    }

    public void addParser(NewsParser parser) {
        parsers.add(parser);
    }

    public void removeAllSources() {
        parsers.clear();
        System.out.println("Все источники удалены.");
    }

    public boolean removeSource(String key) {
        try {
            int index = Integer.parseInt(key) - 1;
            if (index >= 0 && index < parsers.size()) {
                String name = parsers.get(index).getSourceName();
                parsers.remove(index);
                System.out.println("Источник \"" + name + "\" удалён.");
                return true;
            }
        } catch (NumberFormatException ignored) {}
        System.out.println("Источник с номером " + key + " не найден.");
        return false;
    }

    public void showAvailableSources() {
        System.out.println("\nДоступные источники новостей:");
        for (int i = 0; i < parsers.size(); i++) {
            System.out.println((i + 1) + ". " + parsers.get(i).getSourceName());
        }
        System.out.println((parsers.size() + 1) + ". Все источники");
    }

    public int parseBySource(String choice) {
        int totalSaved = 0;

        int allIndex = parsers.size() + 1;
        try {
            int selected = Integer.parseInt(choice);
            if (selected == allIndex) {
                System.out.println("Парсинг всех источников...");
                for (NewsParser parser : parsers) {
                    totalSaved += fetchAndSave(parser);
                }
            } else if (selected >= 1 && selected <= parsers.size()) {
                totalSaved = fetchAndSave(parsers.get(selected - 1));
            } else {
                System.out.println("Неверный выбор источника.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Неверный выбор источника.");
        }

        System.out.println("Всего сохранено новых новостей: " + totalSaved);
        return totalSaved;
    }

    private int fetchAndSave(NewsParser parser) {
        System.out.println("\nПарсинг источника: " + parser.getSourceName());
        List<News> list = parser.parseNews();

        int saved = 0;
        for (News news : list) {
            if (repository.save(news)) {
                saved++;
            }
        }

        System.out.println("Сохранено новостей с " + parser.getSourceName() + ": " + saved);
        return saved;
    }

    public void parseAndSave() {
        parseBySource(String.valueOf(parsers.size() + 1));
    }

    public List<String> getSourceKeys() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < parsers.size(); i++) {
            keys.add(String.valueOf(i + 1));
        }
        return keys;
    }

    public int getSourceCount() {
        return parsers.size();
    }
}
