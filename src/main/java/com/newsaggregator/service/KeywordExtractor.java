package com.newsaggregator.service;

import java.util.*;
import java.util.stream.Collectors;

public class KeywordExtractor {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "и", "в", "на", "с", "по", "для", "от", "до", "из", "к", "о", "об",
            "что", "как", "это", "но", "а", "или", "же", "за", "при", "под",
            "над", "через", "без", "после", "во", "не", "то", "так", "все",
            "он", "она", "они", "мы", "вы", "я", "его", "её", "их", "нас",
            "был", "была", "были", "будет", "есть", "также", "уже", "ещё",
            "более", "менее", "очень", "самый", "который", "которая", "которые",
            "the", "a", "an", "is", "are", "was", "were", "in", "on", "at", "of"
    ));

    public List<String> extract(String title, String description) {
        String text = (title == null ? "" : title) + " " + (description == null ? "" : description);
        String[] words = text.toLowerCase().split("[^а-яёa-z]+");

        Map<String, Integer> frequency = new LinkedHashMap<>();
        for (String word : words) {
            if (word.length() >= 4 && !STOP_WORDS.contains(word)) {
                frequency.merge(word, 1, Integer::sum);
            }
        }

        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
