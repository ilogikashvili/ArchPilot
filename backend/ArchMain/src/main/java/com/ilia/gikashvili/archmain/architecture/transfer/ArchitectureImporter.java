package com.ilia.gikashvili.archmain.architecture.transfer;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;

/**
 * Strategy interface for importing architecture documents from various formats.
 */
public interface ArchitectureImporter {

    String getFormat();

    ArchitectureDocument parse(byte[] content);
}
