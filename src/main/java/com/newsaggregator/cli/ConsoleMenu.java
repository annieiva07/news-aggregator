package com.newsaggregator.cli;

import com.newsaggregator.model.News;
import com.newsaggregator.parser.ParserManager;
import com.newsaggregator.service.AnalyticsService;
import com.newsaggregator.service.SchedulerService;
import com.newsaggregator.storage.NewsRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConsoleMenu {

    private final NewsRepository repository;
    private final ParserManager parserManager;
    private final AnalyticsService analyticsService;
    private final SchedulerService schedulerService;
    private final Scanner scanner;

    public ConsoleMenu(NewsRepository repository, ParserManager parserManager,
                       AnalyticsService analyticsService, SchedulerService schedulerService) {
        this.repository = repository;
        this.parserManager = parserManager;
        this.analyticsService = analyticsService;
        this.schedulerService = schedulerService;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> showAllNews();
                case "2" -> searchByKeyword();
                case "3" -> filterByCategory();
                case "4" -> filterBySource();
                case "5" -> filterByDateRange();
                case "6" -> showSorted();
                case "7" -> showStatistics();
                case "8" -> showAnalytics();
                case "9" -> parseNewsWithSourceChoice();
                case "10" -> manageAutoUpdate();
                case "11" -> manageSources();
                case "12" -> exportNews();
                case "13" -> addTestNews();
                case "14" -> clearDatabase();
                case "0" -> {
                    System.out.println("Выход из программы.");
                    return;
                }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n===== НОВОСТНОЙ АГРЕГАТОР =====");
        System.out.println("1.  Показать все новости");
        System.out.println("2.  Поиск по ключевым словам");
        System.out.println("3.  Фильтр по категории");
        System.out.println("4.  Фильтр по источнику");
        System.out.println("5.  Фильтр по дате");
        System.out.println("6.  Сортировка новостей");
        System.out.println("7.  Статистика по категориям");
        System.out.println("8.  Аналитика и тренды");
        System.out.println("9.  Парсить новости");
        System.out.println("10. Автообновление [" + (schedulerService.isRunning() ? "ВКЛ" : "ВЫКЛ") + "]");
        System.out.println("11. Управление источниками");
        System.out.println("12. Экспорт данных (CSV/JSON/HTML)");
        System.out.println("13. Добавить новость вручную");
        System.out.println("14. Очистить базу данных");
        System.out.println("0.  Выход");
        System.out.print("Выберите действие: ");
    }

    private void showAllNews() {
        List<News> newsList = repository.findAll();
        if (newsList.isEmpty()) {
            System.out.println("Новостей нет.");
            return;
        }
        System.out.println("\n===== ВСЕ НОВОСТИ =====");
        printNewsList(newsList);
    }

    private void searchByKeyword() {
        System.out.print("Введите ключевое слово для поиска: ");
        String keyword = scanner.nextLine().trim();
        if (keyword.isEmpty()) {
            System.out.println("Ключевое слово не может быть пустым.");
            return;
        }
        List<News> results = repository.searchByKeyword(keyword);
        if (results.isEmpty()) {
            System.out.println("Новости по запросу \"" + keyword + "\" не найдены.");
            return;
        }
        System.out.println("\n===== РЕЗУЛЬТАТЫ ПОИСКА: \"" + keyword + "\" =====");
        printNewsList(results);
    }

    private void filterByCategory() {
        List<String> categories = repository.findAllCategories();
        if (categories.isEmpty()) {
            System.out.println("Категории не найдены.");
            return;
        }
        System.out.println("\nДоступные категории:");
        for (int i = 0; i < categories.size(); i++) {
            System.out.println((i + 1) + ". " + categories.get(i));
        }
        System.out.print("Выберите категорию (номер или название): ");
        String input = scanner.nextLine().trim();
        String category = resolveByIndex(input, categories);

        List<News> results = repository.findByCategory(category);
        if (results.isEmpty()) {
            System.out.println("Новости в категории \"" + category + "\" не найдены.");
            return;
        }
        System.out.println("\n===== КАТЕГОРИЯ: " + category + " =====");
        printNewsList(results);
    }

    private void filterBySource() {
        List<String> sources = repository.findAllSources();
        if (sources.isEmpty()) {
            System.out.println("Источники не найдены.");
            return;
        }
        System.out.println("\nДоступные источники:");
        for (int i = 0; i < sources.size(); i++) {
            System.out.println((i + 1) + ". " + sources.get(i));
        }
        System.out.print("Выберите источник (номер или название): ");
        String input = scanner.nextLine().trim();
        String source = resolveByIndex(input, sources);

        List<News> results = repository.findBySource(source);
        if (results.isEmpty()) {
            System.out.println("Новости от источника \"" + source + "\" не найдены.");
            return;
        }
        System.out.println("\n===== ИСТОЧНИК: " + source + " =====");
        printNewsList(results);
    }

    private void filterByDateRange() {
        System.out.println("\n===== ФИЛЬТР ПО ДАТЕ =====");
        System.out.println("Формат: yyyy-MM-ddTHH:mm:ss  (пример: 2026-01-01T00:00:00)");
        System.out.print("Дата начала: ");
        String startStr = scanner.nextLine().trim();
        System.out.print("Дата конца: ");
        String endStr = scanner.nextLine().trim();

        try {
            LocalDateTime start = LocalDateTime.parse(startStr);
            LocalDateTime end = LocalDateTime.parse(endStr);
            List<News> results = repository.findByDateRange(start, end);
            if (results.isEmpty()) {
                System.out.println("Новости за указанный период не найдены.");
                return;
            }
            System.out.println("\n===== НОВОСТИ ЗА ПЕРИОД =====");
            printNewsList(results);
        } catch (DateTimeParseException e) {
            System.out.println("Неверный формат даты. Используйте: yyyy-MM-ddTHH:mm:ss");
        }
    }

    private void showSorted() {
        System.out.println("\n===== СОРТИРОВКА =====");
        System.out.println("1. По дате (новые первые)");
        System.out.println("2. По популярности (просмотры)");
        System.out.println("3. По источнику");
        System.out.println("4. По категории");
        System.out.print("Выберите сортировку: ");
        String choice = scanner.nextLine().trim();

        String sortField = switch (choice) {
            case "2" -> "views";
            case "3" -> "source";
            case "4" -> "category";
            default -> "date";
        };

        List<News> newsList = repository.findAllSortedBy(sortField);
        if (newsList.isEmpty()) {
            System.out.println("Новостей нет.");
            return;
        }
        System.out.println("\n===== НОВОСТИ (СОРТИРОВКА: " + sortField.toUpperCase() + ") =====");
        printNewsList(newsList);
    }

    private void showStatistics() {
        System.out.println("\n===== СТАТИСТИКА =====");
        Map<String, Integer> byCategory = analyticsService.getCountByCategory();
        if (byCategory.isEmpty()) {
            System.out.println("Нет данных для статистики.");
            return;
        }
        System.out.println("Количество новостей по категориям:");
        int total = 0;
        for (Map.Entry<String, Integer> entry : byCategory.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            total += entry.getValue();
        }
        System.out.println("Всего новостей в базе: " + total);
    }

    private void showAnalytics() {
        System.out.println("\n===== АНАЛИТИКА И ТРЕНДЫ =====");
        System.out.println("1. Топ ключевых слов за период");
        System.out.println("2. Динамика по ключевому слову");
        System.out.print("Выберите: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            System.out.print("За сколько дней (например, 7): ");
            String daysStr = scanner.nextLine().trim();
            int days = 7;
            try { days = Integer.parseInt(daysStr); } catch (NumberFormatException ignored) {}

            LocalDateTime from = LocalDateTime.now().minusDays(days);
            LocalDateTime to = LocalDateTime.now();
            List<Map.Entry<String, Long>> top = analyticsService.getTrendingKeywords(10, from, to);

            if (top.isEmpty()) {
                System.out.println("Нет данных за период.");
                return;
            }
            System.out.println("\nТоп ключевых слов за " + days + " дней:");
            int rank = 1;
            for (Map.Entry<String, Long> entry : top) {
                System.out.printf("  %2d. %-20s — %d упоминаний%n", rank++, entry.getKey(), entry.getValue());
            }
        } else if (choice.equals("2")) {
            System.out.print("Введите ключевое слово: ");
            String keyword = scanner.nextLine().trim();
            System.out.print("За сколько дней: ");
            String daysStr = scanner.nextLine().trim();
            int days = 7;
            try { days = Integer.parseInt(daysStr); } catch (NumberFormatException ignored) {}

            Map<String, Integer> dynamics = analyticsService.getKeywordDynamics(keyword, days);
            System.out.println("\nДинамика по \"" + keyword + "\":");
            dynamics.forEach((date, count) ->
                    System.out.printf("  %s: %s%n", date, "█".repeat(count) + " (" + count + ")")
            );
        } else {
            System.out.println("Неверный выбор.");
        }
    }

    private void parseNewsWithSourceChoice() {
        System.out.println("\n===== ПАРСИНГ НОВОСТЕЙ =====");
        parserManager.showAvailableSources();
        System.out.print("Выберите источник: ");
        String choice = scanner.nextLine().trim();

        int max = parserManager.getSourceCount() + 1;
        int selected;
        try {
            selected = Integer.parseInt(choice);
        } catch (NumberFormatException e) {
            System.out.println("Неверный выбор.");
            return;
        }
        if (selected < 1 || selected > max) {
            System.out.println("Неверный выбор.");
            return;
        }

        System.out.println("Начинаем сбор новостей...");
        parserManager.parseBySource(choice);
        System.out.println("Парсинг завершён.");
    }

    private void manageAutoUpdate() {
        System.out.println("\n===== АВТООБНОВЛЕНИЕ =====");
        if (schedulerService.isRunning()) {
            System.out.println("Статус: ВКЛ (каждые " + schedulerService.getIntervalMinutes() + " мин.)");
            System.out.println("1. Остановить");
            System.out.println("2. Изменить интервал");
        } else {
            System.out.println("Статус: ВЫКЛ");
            System.out.println("1. Запустить");
        }
        System.out.print("Выберите: ");
        String choice = scanner.nextLine().trim();

        if (schedulerService.isRunning()) {
            if (choice.equals("1")) {
                schedulerService.stop();
            } else if (choice.equals("2")) {
                System.out.print("Интервал в минутах: ");
                String intervalStr = scanner.nextLine().trim();
                try {
                    int interval = Integer.parseInt(intervalStr);
                    if (interval <= 0) {
                        System.out.println("Ошибка: интервал должен быть положительным числом.");
                    } else {
                        schedulerService.start(interval);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Неверный формат числа.");
                }
            }
        } else {
            if (choice.equals("1")) {
                System.out.print("Интервал в минутах (по умолчанию 30): ");
                String intervalStr = scanner.nextLine().trim();
                int interval = 30;
                try {
                    int parsed = Integer.parseInt(intervalStr);
                    if (parsed > 0) {
                        interval = parsed;
                    } else {
                        System.out.println("Ошибка: интервал должен быть положительным числом.");
                        return;
                    }
                } catch (NumberFormatException ignored) {}
                schedulerService.start(interval);
            }
        }
    }

    private void manageSources() {
        System.out.println("\n===== УПРАВЛЕНИЕ ИСТОЧНИКАМИ =====");
        System.out.println("1. Показать источники");
        System.out.println("2. Добавить источник (RSS URL)");
        System.out.println("3. Удалить источник");
        System.out.print("Выберите: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> parserManager.showAvailableSources();
            case "2" -> {
                System.out.print("Название источника: ");
                String name = scanner.nextLine().trim();
                System.out.print("RSS URL: ");
                String url = scanner.nextLine().trim();
                if (name.isEmpty() || url.isEmpty()) {
                    System.out.println("Название и URL не могут быть пустыми.");
                } else if (!isValidUrl(url)) {
                    System.out.println("Неверный формат URL. Укажите корректный адрес (например: https://example.com/rss).");
                } else {
                    parserManager.addSource(name, url);
                }
            }
            case "3" -> {
                parserManager.showAvailableSources();
                System.out.print("Номер источника для удаления: ");
                String key = scanner.nextLine().trim();
                try {
                    int selected = Integer.parseInt(key);
                    if (selected == parserManager.getSourceCount() + 1) {
                        parserManager.removeAllSources();
                    } else {
                        parserManager.removeSource(key);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Неверный формат номера.");
                }
            }
            default -> System.out.println("Неверный выбор.");
        }
    }

    private void exportNews() {
        System.out.println("\n===== ЭКСПОРТ ДАННЫХ =====");
        System.out.println("1. CSV");
        System.out.println("2. JSON");
        System.out.println("3. HTML");
        System.out.print("Выберите формат: ");
        String format = scanner.nextLine().trim();

        System.out.print("Имя файла (без расширения): ");
        String fileName = scanner.nextLine().trim();
        if (fileName.isEmpty()) {
            fileName = "news_export";
        }

        try {
            switch (format) {
                case "1" -> {
                    repository.exportToCsv(fileName + ".csv");
                    System.out.println("Экспорт выполнен: " + fileName + ".csv");
                }
                case "2" -> {
                    repository.exportToJson(fileName + ".json");
                    System.out.println("Экспорт выполнен: " + fileName + ".json");
                }
                case "3" -> {
                    repository.exportToHtml(fileName + ".html");
                    System.out.println("Экспорт выполнен: " + fileName + ".html");
                }
                default -> System.out.println("Неверный выбор формата.");
            }
        } catch (IOException e) {
            System.err.println("Ошибка экспорта: " + e.getMessage());
        }
    }

    private void addTestNews() {
        System.out.println("\n===== ДОБАВЛЕНИЕ НОВОСТИ ВРУЧНУЮ =====");
        System.out.print("Заголовок: ");
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) {
            System.out.println("Заголовок не может быть пустым.");
            return;
        }
        System.out.print("Краткое описание: ");
        String description = scanner.nextLine().trim();
        System.out.print("Полный текст: ");
        String fullText = scanner.nextLine().trim();
        System.out.print("Источник (название сайта): ");
        String sourceName = scanner.nextLine().trim();
        System.out.print("Категория: ");
        String category = scanner.nextLine().trim();
        System.out.print("Ссылка на источник (необязательно, Enter чтобы пропустить): ");
        String sourceUrl = scanner.nextLine().trim();
        if (sourceUrl.isEmpty()) {
            sourceUrl = "manual:" + System.currentTimeMillis();
        }

        News news = new News(
                title, description, fullText,
                LocalDateTime.now(),
                sourceUrl,
                sourceName
        );
        news.setCategory(category.isEmpty() ? "Без категории" : category);

        if (repository.save(news)) {
            System.out.println("Новость успешно добавлена!");
        } else {
            System.out.println("Не удалось добавить новость.");
        }
    }

    private void clearDatabase() {
        System.out.print("Вы уверены? Все новости будут удалены. (да/нет): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("да")) {
            repository.deleteAll();
        } else {
            System.out.println("Отменено.");
        }
    }

    private void printNewsList(List<News> list) {
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).toShortString(i + 1));
            System.out.println("---");
        }
        System.out.println("Всего: " + list.size());
    }






    private boolean isValidUrl(String url) {
        try {
            URL parsed = new URL(url);
            String protocol = parsed.getProtocol();
            return (protocol.equals("http") || protocol.equals("https")) && !parsed.getHost().isEmpty();
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private String resolveByIndex(String input, List<String> list) {
        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
        } catch (NumberFormatException ignored) {}
        return input;
    }
}
