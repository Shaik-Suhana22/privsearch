package com.privsearch.repository;

import com.privsearch.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Integer> {

    @Query("SELECT d FROM Document d WHERE LOWER(d.filename) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Document> search(@Param("keyword") String keyword);
}