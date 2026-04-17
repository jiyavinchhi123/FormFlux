package com.example.dynamicform.controller;

import java.util.List;
import java.util.UUID;
import java.io.ByteArrayOutputStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.example.dynamicform.model.Form;
import com.example.dynamicform.model.Response;
import com.example.dynamicform.repository.FormRepository;
import com.example.dynamicform.repository.ResponseRepository;

// REST controller that exposes all API endpoints
@RestController
@CrossOrigin(origins = "*") // Allow frontend to call these APIs
public class FormController {

    private final FormRepository formRepository;
    private final ResponseRepository responseRepository;

    public FormController(FormRepository formRepository, ResponseRepository responseRepository) {
        this.formRepository = formRepository;
        this.responseRepository = responseRepository;
    }

    // 1. POST /create-form -> create a new form
    @PostMapping("/create-form")
    public ResponseEntity<CreateFormResponse> createForm(@RequestBody Form form) {
        // Require simple owner login for dashboard access
        if (isBlank(form.getOwnerUsername()) || isBlank(form.getOwnerPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Generate a secret key only the owner should know
        form.setOwnerKey(UUID.randomUUID().toString());
        Form saved = formRepository.save(form);

        // Return helpful links for sharing
        String formId = saved.getId();
        String shareUrl = "/form.html?id=" + formId;
        String ownerUrl = "/dashboard.html?id=" + formId;

        return ResponseEntity.ok(new CreateFormResponse(formId, saved.getOwnerKey(), shareUrl, ownerUrl));
    }

    // 2. GET /form/{id} -> get form by id (public, no owner key)
    @GetMapping("/form/{id}")
    public ResponseEntity<PublicForm> getFormById(@PathVariable String id) {
        return formRepository.findById(id)
                .map(form -> ResponseEntity.ok(new PublicForm(form.getId(), form.getTitle(), form.getFields())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 3. POST /submit/{formId} -> submit response (public)
    @PostMapping("/submit/{formId}")
    public Response submitResponse(@PathVariable String formId, @RequestBody Response response) {
        response.setFormId(formId); // ensure correct formId from URL
        response.setResponses(sanitizeKeys(response.getResponses()));
        if (response.getCreatedAt() == null) {
            response.setCreatedAt(java.time.Instant.now());
        }
        return responseRepository.save(response);
    }

    // 4. GET /responses/{formId} -> get all responses (owner only)
    @GetMapping("/responses/{formId}")
    public ResponseEntity<List<Response>> getResponsesByForm(
            @PathVariable String formId,
            @RequestParam String ownerUser,
            @RequestParam String ownerPass) {

        return formRepository.findById(formId)
                .map(form -> {
                    if (!isOwnerAuthorized(form, ownerUser, ownerPass)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<List<Response>>build();
                    }
                    List<Response> responses = responseRepository.findByFormId(formId);
                    return ResponseEntity.ok(responses);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).<List<Response>>build());
    }

    // 5. POST /regenerate-owner-key/{formId} -> owner can rotate key
    @PostMapping("/regenerate-owner-key/{formId}")
    public ResponseEntity<OwnerKeyResponse> regenerateOwnerKey(
            @PathVariable String formId,
            @RequestParam String ownerUser,
            @RequestParam String ownerPass) {

        return formRepository.findById(formId)
                .map(form -> {
                    if (!isOwnerAuthorized(form, ownerUser, ownerPass)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<OwnerKeyResponse>build();
                    }
                    form.setOwnerKey(UUID.randomUUID().toString());
                    Form saved = formRepository.save(form);
                    String ownerUrl = "/dashboard.html?id=" + saved.getId() + "&ownerKey=" + saved.getOwnerKey();
                    return ResponseEntity.ok(new OwnerKeyResponse(saved.getOwnerKey(), ownerUrl));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).<OwnerKeyResponse>build());
    }

    // 6. GET /responses/{formId}/xlsx -> download responses as XLSX (owner only)
    @GetMapping("/responses/{formId}/xlsx")
    public ResponseEntity<byte[]> downloadResponsesXlsx(
            @PathVariable String formId,
            @RequestParam String ownerUser,
            @RequestParam String ownerPass) {

        return formRepository.findById(formId)
                .map(form -> {
                    if (!isOwnerAuthorized(form, ownerUser, ownerPass)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<byte[]>build();
                    }

                    List<Response> responses = responseRepository.findByFormId(formId);
                    byte[] fileBytes = buildXlsx(form, responses);

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                            .header("Content-Disposition", "attachment; filename=\"responses.xlsx\"")
                            .body(fileBytes);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).<byte[]>build());
    }

    private boolean isOwnerAuthorized(Form form, String ownerUser, String ownerPass) {
        // Require owner username + password
        String user = form.getOwnerUsername();
        String pass = form.getOwnerPassword();
        return user != null && pass != null && user.equals(ownerUser) && pass.equals(ownerPass);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // MongoDB does not allow dots or $ in map keys. Replace them recursively.
    private java.util.Map<String, Object> sanitizeKeys(java.util.Map<String, Object> input) {
        java.util.Map<String, Object> safe = new java.util.LinkedHashMap<>();
        if (input == null) {
            return safe;
        }
        for (java.util.Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey();
            key = key.replace(".", "_").replace("$", "_");
            if (key.isEmpty()) {
                key = "field";
            }
            safe.put(key, sanitizeValue(entry.getValue()));
        }
        return safe;
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof java.util.Map<?, ?> mapVal) {
            java.util.Map<String, Object> safe = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<?, ?> e : mapVal.entrySet()) {
                String key = e.getKey() == null ? "" : String.valueOf(e.getKey());
                key = key.replace(".", "_").replace("$", "_");
                if (key.isEmpty()) {
                    key = "field";
                }
                safe.put(key, sanitizeValue(e.getValue()));
            }
            return safe;
        }
        if (value instanceof java.util.List<?> listVal) {
            java.util.List<Object> safeList = new java.util.ArrayList<>();
            for (Object item : listVal) {
                safeList.add(sanitizeValue(item));
            }
            return safeList;
        }
        return value;
    }

    private byte[] buildXlsx(Form form, List<Response> responses) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Responses");

            // Header row based on form fields
            Row header = sheet.createRow(0);
            List<com.example.dynamicform.model.Field> fields = form.getFields();
            // First column for timestamp
            Cell timeHeader = header.createCell(0);
            timeHeader.setCellValue("Submitted At");
            for (int i = 0; i < fields.size(); i++) {
                Cell cell = header.createCell(i + 1);
                cell.setCellValue(fields.get(i).getLabel());
            }

            // Data rows
            for (int r = 0; r < responses.size(); r++) {
                Row row = sheet.createRow(r + 1);
                java.util.Map<String, Object> map = responses.get(r).getResponses();

                // Submitted At in first column
                Cell timeCell = row.createCell(0);
                java.time.Instant createdAt = responses.get(r).getCreatedAt();
                timeCell.setCellValue(createdAt == null ? "" : createdAt.toString());

                for (int c = 0; c < fields.size(); c++) {
                    String label = fields.get(c).getLabel();
                    String safeKey = label.replace(".", "_").replace("$", "_");
                    Object val = map != null ? map.get(safeKey) : null;
                    Cell cell = row.createCell(c + 1);
                    cell.setCellValue(val == null ? "" : String.valueOf(val));
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    // Simple DTO for public form data (no ownerKey exposed)
    public static class PublicForm {
        private String id;
        private String title;
        private List<?> fields;

        public PublicForm(String id, String title, List<?> fields) {
            this.id = id;
            this.title = title;
            this.fields = fields;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public List<?> getFields() {
            return fields;
        }
    }

    // Response returned after creating a form (includes share links)
    public static class CreateFormResponse {
        private String formId;
        private String ownerKey;
        private String shareUrl;
        private String ownerUrl;

        public CreateFormResponse(String formId, String ownerKey, String shareUrl, String ownerUrl) {
            this.formId = formId;
            this.ownerKey = ownerKey;
            this.shareUrl = shareUrl;
            this.ownerUrl = ownerUrl;
        }

        public String getFormId() {
            return formId;
        }

        public String getOwnerKey() {
            return ownerKey;
        }

        public String getShareUrl() {
            return shareUrl;
        }

        public String getOwnerUrl() {
            return ownerUrl;
        }
    }

    // Response returned after owner key rotation
    public static class OwnerKeyResponse {
        private String ownerKey;
        private String ownerUrl;

        public OwnerKeyResponse(String ownerKey, String ownerUrl) {
            this.ownerKey = ownerKey;
            this.ownerUrl = ownerUrl;
        }

        public String getOwnerKey() {
            return ownerKey;
        }

        public String getOwnerUrl() {
            return ownerUrl;
        }
    }
}
