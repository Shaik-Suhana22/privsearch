//applicaiton logic
package com.privsearch.service;

import com.privsearch.Document;
import com.privsearch.repository.DocumentRepository;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }
    public java.util.List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }
    public java.util.List<Document> searchDocuments(String keyword) {

        String[] words = keyword.toLowerCase().split("\\s+");

        java.util.List<Document> results =
                documentRepository.search(words[0]);

        for (int i = 1; i < words.length; i++) {

            java.util.List<Document> wordResults =
                    documentRepository.search(words[i]);

            results.removeIf(document ->
                    wordResults.stream()
                            .noneMatch(other ->
                                    other.getId().equals(document.getId())
                            )
            );
        }

        return results;
    }

    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }
}