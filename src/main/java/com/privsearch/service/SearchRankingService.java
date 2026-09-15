package com.privsearch.service;

import com.privsearch.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchRankingService {

    public List<Document> rankDocuments(String keyword, List<Document> documents) {

        // Clean and split the search query into individual words
        String cleanedKeyword = keyword.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim();

        if (cleanedKeyword.isEmpty()) {
            return documents;
        }

        String[] searchWords = cleanedKeyword.split("\\s+");

        // Calculate the score for EVERY document
        for (Document document : documents) {

            double contentScore = calculateScore(
                    document.getContent(),
                    searchWords,
                    documents
            );

            double filenameScore = calculateFilenameScore(
                    document.getFilename(),
                    searchWords
            );

            double finalScore = contentScore + filenameScore;

            document.setSearchScore(finalScore);
        }

        // Sort highest score first
        documents.sort((a, b) ->
                Double.compare(
                        b.getSearchScore(),
                        a.getSearchScore()
                )
        );

        return documents;
    }

    private double calculateTF(String text, String word) {

        if (text == null || text.isEmpty()) {
            return 0;
        }

        String[] words = text.toLowerCase().split("\\W+");

        int count = 0;

        for (String currentWord : words) {

            if (currentWord.equals(word)) {
                count++;
            }
        }

        return (double) count / words.length;
    }

    private double calculateIDF(String word, List<Document> documents) {

        int documentsContainingWord = 0;

        for (Document document : documents) {

            if (document.getContent() != null &&
                    document.getContent()
                            .toLowerCase()
                            .matches(".*\\b" + java.util.regex.Pattern.quote(word) + "\\b.*")) {

                documentsContainingWord++;
            }
        }

        if (documentsContainingWord == 0) {
            return 0;
        }

        return Math.log(
                (double) documents.size() / documentsContainingWord
        );
    }

    private double calculateScore(
            String text,
            String[] searchWords,
            List<Document> documents) {

        double totalScore = 0;

        for (String word : searchWords) {

            double tf = calculateTF(text, word);

            double idf = calculateIDF(
                    word,
                    documents
            );

            if (tf > 0) {

                // TF-IDF score
                totalScore += tf * idf;

                // Bonus for matching the search word
                totalScore += 1.0;
            }
        }

        return totalScore;
    }

    private double calculateFilenameScore(
            String filename,
            String[] searchWords) {

        if (filename == null || filename.isEmpty()) {
            return 0;
        }

        String lowerFilename = filename.toLowerCase();

        double score = 0;

        for (String word : searchWords) {

            // Strong boost for exact filename match
            if (lowerFilename.equals(word + ".txt")) {

                score += 5.0;

            }
            // Normal filename match
            else if (lowerFilename.contains(word)) {

                score += 2.0;
            }
        }

        return score;
    }
}