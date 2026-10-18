package com.ilia.gikashvili.archmain.architecture.transfer;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;

/**
 * Strategy interface for exporting architecture documents to various formats (JSON, SVG, PDF, etc.).
 */
public interface ArchitectureExporter {

    String getFormat();

    String getContentType();

    byte[] export(ArchitectureDocument document);
}
