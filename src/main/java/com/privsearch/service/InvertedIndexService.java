package com.privsearch.service;

import com.privsearch.Document;
import com.privsearch.repository.DocumentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InvertedIndexService {

    private final Map<String, Set<Integer>> index =
            new HashMap<>();

    private final Map<String, Map<Integer, Integer>> termFrequencyIndex =
            new HashMap<>();

    private final Map<Integer, Integer> documentLengthIndex =
            new HashMap<>();

    private final Map<String, Map<Integer, List<Integer>>> positionalIndex =
            new HashMap<>();

    private final DocumentRepository documentRepository;

    public InvertedIndexService(
            DocumentRepository documentRepository) {

        this.documentRepository = documentRepository;
    }

    @PostConstruct
    public void buildIndex() {

        index.clear();
        termFrequencyIndex.clear();
        documentLengthIndex.clear();
        positionalIndex.clear();

        List<Document> documents =
                documentRepository.findAll();

        for (Document document : documents) {
            indexDocument(document);
        }

        System.out.println(
                "Inverted index built successfully. Documents indexed: "
                        + documents.size()
        );
    }

    public void indexDocument(Document document) {

        if (document == null || document.getId() == null) {
            return;
        }

        // Remove old index data before re-indexing
        removeDocument(document.getId());

        String text = "";

        if (document.getFilename() != null) {
            text += " " + document.getFilename();
        }

        if (document.getContent() != null) {
            text += " " + document.getContent();
        }

        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .trim()
                .split("\\s+");

        int documentLength = 0;

        for (int position = 0;
             position < words.length;
             position++) {

            String word = words[position];

            if (word.isBlank()) {
                continue;
            }

            documentLength++;

            // Normal inverted index
            index
                    .computeIfAbsent(
                            word,
                            key -> new HashSet<>()
                    )
                    .add(document.getId());

            // Term frequency index
            termFrequencyIndex
                    .computeIfAbsent(
                            word,
                            key -> new HashMap<>()
                    )
                    .merge(
                            document.getId(),
                            1,
                            Integer::sum
                    );

            // Positional index
            positionalIndex
                    .computeIfAbsent(
                            word,
                            key -> new HashMap<>()
                    )
                    .computeIfAbsent(
                            document.getId(),
                            key -> new ArrayList<>()
                    )
                    .add(position);
        }

        // Store document length
        documentLengthIndex.put(
                document.getId(),
                documentLength
        );
    }

    public Set<Integer> search(String word) {

        if (word == null || word.isBlank()) {
            return Set.of();
        }

        String normalizedWord =
                word.toLowerCase()
                        .replaceAll("[^a-z0-9]", "");

        return index.getOrDefault(
                normalizedWord,
                Set.of()
        );
    }

    public Set<Integer> searchPhrase(
            List<String> phraseWords) {

        if (phraseWords == null ||
                phraseWords.isEmpty()) {

            return Set.of();
        }

        Set<Integer> candidates =
                new HashSet<>(
                        search(phraseWords.get(0))
                );

        for (int i = 1;
             i < phraseWords.size();
             i++) {

            Set<Integer> nextWordDocuments =
                    search(phraseWords.get(i));

            candidates.retainAll(
                    nextWordDocuments
            );
        }

        Set<Integer> results =
                new HashSet<>();

        for (Integer documentId : candidates) {

            List<Integer> firstWordPositions =
                    positionalIndex
                            .getOrDefault(
                                    phraseWords.get(0),
                                    Collections.emptyMap()
                            )
                            .getOrDefault(
                                    documentId,
                                    Collections.emptyList()
                            );

            for (Integer position :
                    firstWordPositions) {

                boolean matches = true;

                for (int i = 1;
                     i < phraseWords.size();
                     i++) {

                    List<Integer> positions =
                            positionalIndex
                                    .getOrDefault(
                                            phraseWords.get(i),
                                            Collections.emptyMap()
                                    )
                                    .getOrDefault(
                                            documentId,
                                            Collections.emptyList()
                                    );

                    if (!positions.contains(
                            position + i)) {

                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    results.add(documentId);
                    break;
                }
            }
        }

        return results;
    }

    public void removeDocument(
            Integer documentId) {

        if (documentId == null) {
            return;
        }

        // Remove from normal inverted index
        for (Set<Integer> documentIds :
                index.values()) {

            documentIds.remove(documentId);
        }

        index.entrySet().removeIf(
                entry -> entry.getValue().isEmpty()
        );

        // Remove from term frequency index
        for (Map<Integer, Integer> documentFrequencies :
                termFrequencyIndex.values()) {

            documentFrequencies.remove(documentId);
        }

        termFrequencyIndex.entrySet().removeIf(
                entry -> entry.getValue().isEmpty()
        );

        // Remove from positional index
        for (Map<Integer, List<Integer>> positions :
                positionalIndex.values()) {

            positions.remove(documentId);
        }

        positionalIndex.entrySet().removeIf(
                entry -> entry.getValue().isEmpty()
        );

        // Remove document length
        documentLengthIndex.remove(documentId);
    }

    public void clearIndex() {

        index.clear();
        termFrequencyIndex.clear();
        documentLengthIndex.clear();
        positionalIndex.clear();
    }

    public int getTermFrequency(
            String word,
            Integer documentId) {

        if (word == null ||
                documentId == null) {

            return 0;
        }

        String normalizedWord =
                word.toLowerCase()
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        return termFrequencyIndex
                .getOrDefault(
                        normalizedWord,
                        Collections.emptyMap()
                )
                .getOrDefault(
                        documentId,
                        0
                );
    }

    public int getDocumentFrequency(
            String word) {

        if (word == null ||
                word.isBlank()) {

            return 0;
        }

        String normalizedWord =
                word.toLowerCase()
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        return index
                .getOrDefault(
                        normalizedWord,
                        Collections.emptySet()
                )
                .size();
    }

    public int getDocumentLength(
            Integer documentId) {

        if (documentId == null) {
            return 0;
        }

        return documentLengthIndex
                .getOrDefault(
                        documentId,
                        0
                );
    }

    public int getTotalDocumentCount() {

        return documentLengthIndex.size();
    }

    public double getAverageDocumentLength() {

        if (documentLengthIndex.isEmpty()) {
            return 1.0;
        }

        long totalLength = 0;

        for (Integer length :
                documentLengthIndex.values()) {

            totalLength += length;
        }

        return (double) totalLength /
                documentLengthIndex.size();
    }
}