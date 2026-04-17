package com.example.dynamicform.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.dynamicform.model.Form;

// Simple MongoDB repository for Form
public interface FormRepository extends MongoRepository<Form, String> {
}
