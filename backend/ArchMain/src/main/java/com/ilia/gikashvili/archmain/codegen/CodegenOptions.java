package com.ilia.gikashvili.archmain.codegen;

/**
 * @param layout       "multi-module" (one Gradle build, module per component) or "per-service" (independent projects)
 * @param projectName  used for the root folder and artifact names
 * @param groupId      base Java package, e.g. com.example
 */
public record CodegenOptions(String layout, String projectName, String groupId) {
    public boolean multiModule() { return !"per-service".equalsIgnoreCase(layout); }
}
