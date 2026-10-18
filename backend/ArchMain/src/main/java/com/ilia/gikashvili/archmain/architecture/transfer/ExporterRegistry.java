package com.ilia.gikashvili.archmain.architecture.transfer;

import com.ilia.gikashvili.archmain.common.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry mapping format string to exporter/importer strategies.
 */
@Component
public class ExporterRegistry {

    private final Map<String, ArchitectureExporter> exporters;
    private final Map<String, ArchitectureImporter> importers;

    public ExporterRegistry(List<ArchitectureExporter> exporterList,
                            List<ArchitectureImporter> importerList) {
        this.exporters = exporterList.stream()
                .collect(Collectors.toMap(e -> e.getFormat().toLowerCase(), Function.identity()));
        this.importers = importerList.stream()
                .collect(Collectors.toMap(i -> i.getFormat().toLowerCase(), Function.identity()));
    }

    public ArchitectureExporter getExporter(String format) {
        String key = format != null ? format.toLowerCase() : "json";
        ArchitectureExporter exporter = exporters.get(key);
        if (exporter == null) {
            throw new ValidationException("Unsupported export format: " + format + ". Supported formats: " + exporters.keySet());
        }
        return exporter;
    }

    public ArchitectureImporter getImporter(String format) {
        String key = format != null ? format.toLowerCase() : "json";
        ArchitectureImporter importer = importers.get(key);
        if (importer == null) {
            throw new ValidationException("Unsupported import format: " + format + ". Supported formats: " + importers.keySet());
        }
        return importer;
    }
}
