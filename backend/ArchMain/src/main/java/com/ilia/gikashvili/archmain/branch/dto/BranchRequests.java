package com.ilia.gikashvili.archmain.branch.dto;
import java.util.UUID;
import jakarta.validation.constraints.*;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
public final class BranchRequests {
 public record Create(@NotBlank @Size(max=255) String name,@Size(max=2000) String description,UUID fromBranchId) {}
 public record Patch(@Size(min=1,max=255) String name,@Size(max=2000) String description) {}
 public record Save(@NotNull ArchitectureDocument document,@PositiveOrZero Long expectedRevision) {}
 public record Target(UUID targetBranchId,String strategy) {}
}
