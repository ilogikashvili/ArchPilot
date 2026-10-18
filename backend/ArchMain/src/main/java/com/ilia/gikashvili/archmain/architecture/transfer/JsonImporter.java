package com.ilia.gikashvili.archmain.architecture.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.common.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class JsonImporter implements ArchitectureImporter {

    private final ObjectMapper objectMapper;

    public JsonImporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getFormat() {
        return "json";
    }

    @Override
    public ArchitectureDocument parse(byte[] content) {
        try {
            return objectMapper.readValue(content, ArchitectureDocument.class);
        } catch (Exception e) {
            throw new ValidationException("Failed to parse architecture JSON: " + e.getMessage());
        }
    }
}
