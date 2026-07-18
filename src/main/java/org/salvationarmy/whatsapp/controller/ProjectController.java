package org.salvationarmy.whatsapp.controller;

import jakarta.validation.Valid;
import org.salvationarmy.whatsapp.dto.ProjectRequest;
import org.salvationarmy.whatsapp.dto.ProjectResponse;
import org.salvationarmy.whatsapp.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private org.salvationarmy.whatsapp.repository.UserRepository userRepository;

    private UUID getCurrentUserId(Authentication authentication) {
        String identifier = authentication.getName(); // Could be username or email
        // Try email first
        java.util.Optional<org.salvationarmy.whatsapp.entity.User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isPresent()) {
            return userOpt.get().getId();
        }
        // Try username if email lookup failed
        userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isPresent()) {
            return userOpt.get().getId();
        }
        throw new RuntimeException("User not found for identifier: " + identifier);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProjectResponse>> getActiveProjects() {
        return ResponseEntity.ok(projectService.getActiveProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable UUID id) {
        try {
            ProjectResponse project = projectService.getProjectById(id);
            return ResponseEntity.ok(project);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Project not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping
    public ResponseEntity<?> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        try {
            UUID createdByUserId = getCurrentUserId(authentication);
            
            // Check if user has permission (ADMIN only)
            org.salvationarmy.whatsapp.entity.User user = userRepository.findById(createdByUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!user.getRole().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Only ADMIN can create projects");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            ProjectResponse response = projectService.createProject(request, createdByUserId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create project");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            
            // Check if user has permission (ADMIN only)
            org.salvationarmy.whatsapp.entity.User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!user.getRole().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Only ADMIN can update projects");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            ProjectResponse response = projectService.updateProject(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update project");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(
            @PathVariable UUID id,
            Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            
            // Check if user has permission (ADMIN only)
            org.salvationarmy.whatsapp.entity.User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!user.getRole().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Only ADMIN can delete projects");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            projectService.deleteProject(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Project deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete project");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
