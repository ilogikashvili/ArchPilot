package com.ilia.gikashvili.archmain.template.dto;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import jakarta.validation.constraints.Size;
public record TemplateRequest(@Size(max=200) String name, @Size(max=2000) String description, ArchitectureDocument document) {}
