package com.ilia.gikashvili.archmain.codegen;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;

import java.util.Map;

/**
 * Turns an architecture document into a project file tree (path -> file content).
 * Implementations are selected by {@link #language()}; add an LLM-backed implementation
 * later to support any language without changing the controller.
 */
public interface CodeGenerator {
    String language();

    Map<String, String> generate(ArchitectureDocument document, CodegenOptions options);
}
