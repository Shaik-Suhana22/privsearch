package com.privsearch.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class QueryProcessorService {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are",
            "was", "were", "in", "on", "at",
            "to", "of", "for", "and", "or"
    );

    public List<String> processQuery(String query) {

        if (query == null || query.isBlank()) {
            return List.of();
        }

        return Arrays.stream(
                        query.toLowerCase()
                                .replaceAll("[^a-z0-9\\s]", " ")
                                .trim()
                                .split("\\s+")
                )
                .filter(word -> !word.isBlank())
                .filter(word -> !STOP_WORDS.contains(word))
                .distinct()
                .toList();
    }
    public boolean isOrQuery(String query) {

        if (query == null || query.isBlank()) {
            return false;
        }

        return query.toLowerCase().contains(" or ");
    }
    public boolean isPhraseQuery(String query) {

        if (query == null || query.isBlank()) {
            return false;
        }

        String trimmed = query.trim();

        return trimmed.startsWith("\"")
                && trimmed.endsWith("\"")
                && trimmed.length() > 1;
    }
    public String extractPhrase(String query) {

        if (!isPhraseQuery(query)) {
            return "";
        }

        return query.trim()
                .substring(1, query.trim().length() - 1)
                .toLowerCase();
    }
}