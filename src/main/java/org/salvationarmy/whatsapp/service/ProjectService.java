package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.dto.ProjectRequest;
import org.salvationarmy.whatsapp.dto.ProjectResponse;
import org.salvationarmy.whatsapp.entity.Project;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.PaymentRepository;
import org.salvationarmy.whatsapp.repository.ProjectRepository;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProjectResponse> getActiveProjects() {
        return projectRepository.findByStatus(Project.ProjectStatus.ACTIVE).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request, UUID createdByUserId) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setTargetAmount(request.getTargetAmount());
        project.setCollectedAmount(BigDecimal.ZERO);
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(request.getStatus() != null ? 
                Project.ProjectStatus.valueOf(request.getStatus()) : Project.ProjectStatus.ACTIVE);
        project.setCreatedByUserId(createdByUserId);

        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    @Transactional
    public ProjectResponse updateProject(UUID id, ProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setTargetAmount(request.getTargetAmount());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            project.setStatus(Project.ProjectStatus.valueOf(request.getStatus()));
        }

        // Update collected amount from payments
        BigDecimal collected = paymentRepository.sumAmountByProject(id);
        if (collected != null) {
            project.setCollectedAmount(collected);
        }

        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    @Transactional
    public void deleteProject(UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found");
        }
        projectRepository.deleteById(id);
    }

    private ProjectResponse toResponse(Project project) {
        // Update collected amount from payments
        BigDecimal collected = paymentRepository.sumAmountByProject(project.getId());
        if (collected != null) {
            project.setCollectedAmount(collected);
            projectRepository.save(project);
        }

        ProjectResponse.ProjectResponseBuilder builder = ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .targetAmount(project.getTargetAmount())
                .collectedAmount(project.getCollectedAmount() != null ? project.getCollectedAmount() : BigDecimal.ZERO)
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .status(project.getStatus().name())
                .createdByUserId(project.getCreatedByUserId())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt());

        if (project.getCreatedBy() != null) {
            builder.createdByName(project.getCreatedBy().getFullName());
        } else if (project.getCreatedByUserId() != null) {
            userRepository.findById(project.getCreatedByUserId())
                    .ifPresent(user -> builder.createdByName(user.getFullName()));
        }

        return builder.build();
    }
}
