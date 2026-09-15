package com.privsearch;

import com.privsearch.service.DocumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;
import com.privsearch.service.SearchRankingService;

@RestController
public class SearchController {

    private final DocumentService documentService;
    private final SearchRankingService searchRankingService;

    public SearchController(DocumentService documentService,SearchRankingService searchRankingService) {
        this.documentService = documentService;
        this.searchRankingService = searchRankingService;
    }

    @GetMapping("/documents")
    public java.util.List<Document> getDocuments() {
        return documentService.getAllDocuments();
    }
    @GetMapping("/search")
    public java.util.List<Document> search(@RequestParam String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return java.util.List.of();
        }

        java.util.List<Document> documents =
                documentService.searchDocuments(keyword);

        return searchRankingService.rankDocuments(keyword, documents);
    }
    @PostMapping("/documents")
    public Document addDocument(@RequestBody Document document) {
        return documentService.saveDocument(document);
    }
    @PostMapping("/upload")
    public Document uploadFile(@RequestParam("file") MultipartFile file) throws Exception {

        Tika tika = new Tika();

        Document document = new Document();

        document.setFilename(file.getOriginalFilename());
        document.setFiletype(file.getContentType());
        document.setFilepath("uploads/" + file.getOriginalFilename());
        document.setContent(tika.parseToString(file.getInputStream()));

        return documentService.saveDocument(document);
    }
}