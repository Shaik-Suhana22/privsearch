package com.privsearch.service;

import com.privsearch.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchRankingService {

    private final InvertedIndexService invertedIndexService;

    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private static final double FILENAME_BOOST = 2.0;

    public SearchRankingService(
            InvertedIndexService invertedIndexService) {

        this.invertedIndexService = invertedIndexService;
    }

    public List<Document> rankDocuments(
            String keyword,
            List<Document> documents) {

        if (keyword == null ||
                keyword.isBlank() ||
                documents.isEmpty()) {

            return documents;
        }

        String cleanedKeyword = keyword.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .trim();

        if (cleanedKeyword.isEmpty()) {
            return documents;
        }

        String[] searchWords =
                cleanedKeyword.split("\\s+");

        // Average document length across the ENTIRE corpus
        double averageDocumentLength =
                invertedIndexService.getAverageDocumentLength();

        for (Document document : documents) {

            double contentScore =
                    calculateBM25Score(
                            document,
                            searchWords,
                            averageDocumentLength
                    );

            double filenameScore =
                    calculateFilenameScore(
                            document.getFilename(),
                            searchWords
                    );

            double finalScore =
                    contentScore + filenameScore;

            document.setSearchScore(finalScore);
        }

        // Highest relevance first
        documents.sort((a, b) ->
                Double.compare(
                        b.getSearchScore(),
                        a.getSearchScore()
                )
        );

        return documents;
    }

    private double calculateBM25Score(
            Document document,
            String[] searchWords,
            double averageDocumentLength) {

        Integer documentId =
                document.getId();

        int documentLength =
                invertedIndexService.getDocumentLength(
                        documentId
                );

        if (documentLength == 0) {
            return 0;
        }

        double totalScore = 0;

        // Total number of documents in the whole corpus
        int totalDocuments =
                invertedIndexService.getTotalDocumentCount();

        for (String word : searchWords) {

            if (word.isBlank()) {
                continue;
            }

            int termFrequency =
                    invertedIndexService.getTermFrequency(
                            word,
                            documentId
                    );

            if (termFrequency == 0) {
                continue;
            }

            int documentFrequency =
                    invertedIndexService.getDocumentFrequency(
                            word
                    );

            if (documentFrequency == 0) {
                continue;
            }

            // BM25 inverse document frequency
            double idf = Math.log(
                    1.0 +
                            (totalDocuments
                                    - documentFrequency
                                    + 0.5)
                                    /
                                    (documentFrequency + 0.5)
            );

            double numerator =
                    termFrequency * (K1 + 1);

            double denominator =
                    termFrequency
                            + K1 * (
                            1 - B
                                    + B * (
                                    (double) documentLength
                                            / averageDocumentLength
                            )
                    );

            totalScore +=
                    idf * (numerator / denominator);
        }

        return totalScore;
    }

    private double calculateFilenameScore(
            String filename,
            String[] searchWords) {

        if (filename == null ||
                filename.isBlank()) {

            return 0;
        }

        String normalizedFilename =
                filename.toLowerCase()
                        .replaceAll(
                                "[^a-z0-9\\s]",
                                " "
                        )
                        .trim();

        double score = 0;

        for (String word : searchWords) {

            if (normalizedFilename.equals(word)) {

                // Exact filename match
                score += FILENAME_BOOST * 2;

            } else if (normalizedFilename.contains(word)) {

                // Partial filename match
                score += FILENAME_BOOST;
            }
        }

        return score;
    }
}