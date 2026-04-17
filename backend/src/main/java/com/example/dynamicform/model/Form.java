package com.example.dynamicform.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// Represents a form that contains a title and a list of fields
@Document(collection = "forms")
public class Form {
    @Id
    private String id; // MongoDB document id

    private String title;
    private List<Field> fields;

    // Secret key used by the form owner to view responses
    private String ownerKey;

    // Simple owner login (beginner-friendly)
    private String ownerUsername;
    private String ownerPassword;

    public Form() {
    }

    public Form(String title, List<Field> fields) {
        this.title = title;
        this.fields = fields;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getOwnerPassword() {
        return ownerPassword;
    }

    public void setOwnerPassword(String ownerPassword) {
        this.ownerPassword = ownerPassword;
    }
}
