package com.newsaggregator.service;

import com.newsaggregator.model.News;
import com.newsaggregator.storage.NewsRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsService {

    private final NewsRepository repository;

    public AnalyticsService(NewsRepository repository) {
        this.repository = repository;
    }

    public List<Map.Entry<String, Long>> getTrendingKeywords(int topN, LocalDateTime from, LocalDateTime to) {
        List<News> news = repository.findByDateRange(from, to);

        Map<String, Long> frequency = new HashMap<>();
        for (News item : news) {
            for (String keyword : item.getKeywords()) {
                frequency.merge(keyword.toLowerCase(), 1L, Long::sum);
            }
            countTitleWords(item.getTitle(), frequency);
        }

        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    public Map<String, Integer> getKeywordDynamics(String keyword, int days) {
        Map<String, Integer> dynamics = new LinkedHashMap<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime start = LocalDateTime.now().minusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime end = start.plusDays(1).minusSeconds(1);
            List<News> matches = repository.searchByKeyword(keyword).stream()
                    .filter(n -> n.getPublishDate() != null
                            && !n.getPublishDate().isBefore(start)
                            && !n.getPublishDate().isAfter(end))
                    .collect(Collectors.toList());
            dynamics.put(start.toLocalDate().toString(), matches.size());
        }

        return dynamics;
    }

    public Map<String, Integer> getCountByCategory() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String category : repository.findAllCategories()) {
            result.put(category, repository.countByCategory(category));
        }
        return result;
    }

    private void countTitleWords(String title, Map<String, Long> frequency) {
        if (title == null) return;
        String[] words = title.toLowerCase().split("[^а-яёa-z]+");
        for (String word : words) {
            if (word.length() >= 5) {
                frequency.merge(word, 1L, Long::sum);
            }
        }
    }
}
