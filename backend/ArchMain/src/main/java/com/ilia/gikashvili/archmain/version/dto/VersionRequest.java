package com.ilia.gikashvili.archmain.version.dto;
import jakarta.validation.constraints.Size; import java.util.UUID;
public record VersionRequest(@Size(max=200) String name,@Size(max=2000) String description,UUID projectId,@Size(max=2000) String message,UUID branchId) {}
