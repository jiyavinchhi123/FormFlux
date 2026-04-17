package com.example.dynamicform.model;

import java.util.Map;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// Stores a user's responses for a specific form
@Document(collection = "responses")
public class Response {
    @Id
    private String id; // MongoDB document id

    private String formId; // Which form this response belongs to
    private Map<String, Object> responses; // Key = field label, Value = user input
    private Instant createdAt; // When the response was submitted

    public Response() {
    }

    public Response(String formId, Map<String, Object> responses) {
        this.formId = formId;
        this.responses = responses;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public Map<String, Object> getResponses() {
        return responses;
    }

    public void setResponses(Map<String, Object> responses) {
        this.responses = responses;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
