package com.newsaggregator.parser;

import com.newsaggregator.model.News;
import java.util.List;

public interface NewsParser {
    List<News> parseNews();
    String getSourceName();
}