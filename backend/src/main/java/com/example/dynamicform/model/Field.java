package com.example.dynamicform.model;

import java.util.List;

// A single field inside a form (example: text input, number input, dropdown)
public class Field {
    private String label;   // Field label shown to users
    private String type;    // short_answer, paragraph, multiple_choice, checkboxes, dropdown, linear_scale, multiple_choice_grid, checkbox_grid, date, time

    // For choices (multiple choice, checkboxes, dropdown)
    private List<String> options;

    // For grid questions
    private List<String> rows;
    private List<String> columns;

    // For linear scale
    private Integer min;
    private Integer max;
    private String leftLabel;
    private String rightLabel;

    // Required field (basic validation)
    private boolean required;

    public Field() {
    }

    public Field(String label, String type, List<String> options) {
        this.label = label;
        this.type = type;
        this.options = options;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<String> getRows() {
        return rows;
    }

    public void setRows(List<String> rows) {
        this.rows = rows;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public String getLeftLabel() {
        return leftLabel;
    }

    public void setLeftLabel(String leftLabel) {
        this.leftLabel = leftLabel;
    }

    public String getRightLabel() {
        return rightLabel;
    }

    public void setRightLabel(String rightLabel) {
        this.rightLabel = rightLabel;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
