// Application logic
package com.privsearch.service;

import com.privsearch.Document;
import com.privsearch.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final InvertedIndexService invertedIndexService;
    private final QueryProcessorService queryProcessorService;

    public DocumentService(
            DocumentRepository documentRepository,
            InvertedIndexService invertedIndexService,
            QueryProcessorService queryProcessorService) {

        this.documentRepository = documentRepository;
        this.invertedIndexService = invertedIndexService;
        this.queryProcessorService = queryProcessorService;
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public List<Document> searchDocuments(String keyword) {

        List<String> words =
                queryProcessorService.processQuery(keyword);

        if (queryProcessorService.isPhraseQuery(keyword)) {

            return documentRepository.findAllById(
                    invertedIndexService.searchPhrase(
                            words
                    )
            );
        }

        if (words.isEmpty()) {
            return List.of();
        }

        Set<Integer> matchingDocumentIds =
                new HashSet<>();

        if (queryProcessorService.isOrQuery(keyword)) {

            // OR query:
            // A document can contain ANY of the search words.

            for (String word : words) {

                Set<Integer> wordDocumentIds =
                        invertedIndexService.search(word);

                matchingDocumentIds.addAll(wordDocumentIds);
            }

        } else {

            // AND query:
            // A document must contain ALL search words.

            matchingDocumentIds =
                    new HashSet<>(
                            invertedIndexService.search(words.get(0))
                    );

            for (int i = 1; i < words.size(); i++) {

                Set<Integer> wordDocumentIds =
                        invertedIndexService.search(words.get(i));

                matchingDocumentIds.retainAll(wordDocumentIds);
            }
        }

        if (matchingDocumentIds.isEmpty()) {
            return List.of();
        }

        return documentRepository.findAllById(
                matchingDocumentIds
        );
    }
    public Document saveDocument(Document document) {

        Document savedDocument =
                documentRepository.save(document);

        // Add the newly saved document to the search index
        invertedIndexService.indexDocument(savedDocument);

        return savedDocument;
    }
}