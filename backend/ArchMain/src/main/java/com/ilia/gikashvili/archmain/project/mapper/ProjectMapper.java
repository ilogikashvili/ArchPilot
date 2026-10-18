package com.ilia.gikashvili.archmain.project.mapper;

import com.ilia.gikashvili.archmain.project.dto.ProjectResponse;
import com.ilia.gikashvili.archmain.project.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwnerId(),
                project.isArchived(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
