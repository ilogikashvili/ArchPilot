package com.ilia.gikashvili.archmain.architecture.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JsonExporter implements ArchitectureExporter {

    private final ObjectMapper objectMapper;

    public JsonExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getFormat() {
        return "json";
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public byte[] export(ArchitectureDocument document) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export architecture document to JSON", e);
        }
    }
}
