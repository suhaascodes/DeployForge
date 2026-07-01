package com.deployforge.project.service;

import com.deployforge.common.exception.ResourceNotFoundException;
import com.deployforge.config.EncryptionService;
import com.deployforge.project.dto.EnvironmentVariableCreateRequest;
import com.deployforge.project.dto.EnvironmentVariableDto;
import com.deployforge.project.entity.Project;
import com.deployforge.project.entity.ProjectEnvironmentVariable;
import com.deployforge.project.repository.EnvironmentVariableRepository;
import com.deployforge.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EnvironmentVariableService {

    private final EnvironmentVariableRepository environmentVariableRepository;
    private final ProjectRepository projectRepository;
    private final EncryptionService encryptionService;

    public EnvironmentVariableService(EnvironmentVariableRepository environmentVariableRepository,
                                      ProjectRepository projectRepository,
                                      EncryptionService encryptionService) {
        this.environmentVariableRepository = environmentVariableRepository;
        this.projectRepository = projectRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentVariableDto> listVariables(UUID projectId, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or you are not the owner"));

        return environmentVariableRepository.findByProjectIdOrderByKeyAsc(project.getId())
                .stream()
                .map(EnvironmentVariableDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public EnvironmentVariableDto saveOrUpdateVariable(UUID projectId, EnvironmentVariableCreateRequest request, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or you are not the owner"));

        String encryptedVal = encryptionService.encrypt(request.getValue());

        Optional<ProjectEnvironmentVariable> existingOpt = environmentVariableRepository
                .findByProjectIdAndKey(project.getId(), request.getKey());

        ProjectEnvironmentVariable variable;
        if (existingOpt.isPresent()) {
            variable = existingOpt.get();
            variable.setEncryptedValue(encryptedVal);
        } else {
            variable = ProjectEnvironmentVariable.builder()
                    .project(project)
                    .key(request.getKey())
                    .encryptedValue(encryptedVal)
                    .build();
        }

        ProjectEnvironmentVariable saved = environmentVariableRepository.save(variable);
        return EnvironmentVariableDto.fromEntity(saved);
    }

    @Transactional
    public void deleteVariable(UUID projectId, UUID variableId, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or you are not the owner"));

        ProjectEnvironmentVariable variable = environmentVariableRepository.findById(variableId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment variable not found"));

        if (!variable.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Variable does not belong to this project");
        }

        environmentVariableRepository.delete(variable);
    }
}
