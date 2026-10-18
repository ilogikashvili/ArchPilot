package com.ilia.gikashvili.archmain.architecture.mapper;

import com.ilia.gikashvili.archmain.architecture.dto.ArchitectureResponse;
import com.ilia.gikashvili.archmain.architecture.entity.ArchitectureDocumentEntity;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ArchitectureMapper {

    private final ObjectMapper objectMapper;

    public ArchitectureMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ArchitectureDocument toModel(ArchitectureDocumentEntity entity) {
        try {
            return objectMapper.readValue(entity.getDocumentJson(), ArchitectureDocument.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize architecture document", e);
        }
    }

    public String toJson(ArchitectureDocument document) {
        try {
            return objectMapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize architecture document", e);
        }
    }

    public ArchitectureResponse toResponse(ArchitectureDocumentEntity entity) {
        return new ArchitectureResponse(
                entity.getProjectId(),
                toModel(entity),
                entity.getRevision(),
                entity.getUpdatedAt()
        );
    }
}
