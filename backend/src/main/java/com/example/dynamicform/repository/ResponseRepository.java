package com.example.dynamicform.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.dynamicform.model.Response;

// Simple MongoDB repository for Response
public interface ResponseRepository extends MongoRepository<Response, String> {
    // Custom query method to fetch all responses for a form
    List<Response> findByFormId(String formId);
}
