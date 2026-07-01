package com.deployforge.project.service;

import com.deployforge.auth.entity.User;
import com.deployforge.auth.repository.UserRepository;
import com.deployforge.common.exception.ResourceNotFoundException;
import com.deployforge.project.dto.ProjectCreateRequest;
import com.deployforge.project.dto.ProjectResponse;
import com.deployforge.project.entity.Project;
import com.deployforge.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final GitValidationService gitValidationService;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          GitValidationService gitValidationService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.gitValidationService = gitValidationService;
    }

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate Repository URL (validates format and reaches out via JGit)
        gitValidationService.validateRepository(request.getRepositoryUrl());

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .repositoryUrl(request.getRepositoryUrl())
                .owner(owner)
                .webhookSecret("df_sec_" + UUID.randomUUID().toString().replace("-", ""))
                .build();

        Project savedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(UUID ownerId) {
        return projectRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or you are not the owner"));
        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, ProjectCreateRequest request, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or you are not the owner"));

        gitValidationService.validateRepository(request.getRepositoryUrl());

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setRepositoryUrl(request.getRepositoryUrl());

        Project updatedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(updatedProject);
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or you are not the owner"));
        projectRepository.delete(project);
    }
}
