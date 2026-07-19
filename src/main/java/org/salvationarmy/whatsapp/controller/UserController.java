package org.salvationarmy.whatsapp.controller;

import jakarta.validation.Valid;
import org.salvationarmy.whatsapp.dto.ChangePasswordRequest;
import org.salvationarmy.whatsapp.dto.CreateUserRequest;
import org.salvationarmy.whatsapp.dto.UpdateUserRequest;
import org.salvationarmy.whatsapp.dto.UserInfo;
import org.salvationarmy.whatsapp.dto.UserResponse;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.salvationarmy.whatsapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private org.salvationarmy.whatsapp.service.PhotoManagementService photoManagementService;

    /**
     * Get photo status for current user
     * GET /api/users/me/photo-status
     */
    @GetMapping("/me/photo-status")
    public ResponseEntity<?> getPhotoStatus(Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            org.salvationarmy.whatsapp.dto.PhotoStatusResponse status = photoManagementService.getPhotoStatus(userId);
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(404).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    private UUID getCurrentUserId(Authentication authentication) {
        String identifier = authentication.getName(); // Could be username or email
        // Try email first
        Optional<User> userOpt = userRepository.findByEmail(identifier);
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
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        try {
            List<UserResponse> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Failed to list users", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to list users");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(userService.getUserById(id));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponse created = userService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getFullName(),
                    request.getRole());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create user");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserResponse updated = userService.updateUser(
                    id,
                    request.getEmail(),
                    request.getFullName(),
                    request.getRole(),
                    request.getStatus());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update user");
            error.put("message", e.getMessage());
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(error);
        }
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable UUID id, @Valid @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(id, request.getNewPassword());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password updated successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to change password");
            error.put("message", e.getMessage());
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete user");
            error.put("message", e.getMessage());
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(error);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserInfo>> searchUsers(
            @RequestParam String q,
            Authentication authentication) {
        try {
            UUID currentUserId = getCurrentUserId(authentication);
            
            // Search by email or full name (case-insensitive)
            List<User> users = userRepository.findAll().stream()
                    .filter(user -> !user.getId().equals(currentUserId)) // Exclude current user
                    .filter(user -> {
                        String query = q.toLowerCase();
                        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                        String fullName = user.getFullName() != null ? user.getFullName().toLowerCase() : "";
                        return email.contains(query) || fullName.contains(query);
                    })
                    .collect(Collectors.toList());

            List<UserInfo> userInfos = users.stream()
                    .map(user -> new UserInfo(
                            user.getId(),
                            user.getEmail(),
                            user.getFullName(),
                            user.getRole()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(userInfos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            String identifier = authentication.getName(); // Could be username or email
            if (identifier == null || identifier.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication failed");
                error.put("message", "No identifier found in authentication");
                return ResponseEntity.status(401).body(error);
            }
            
            // Try email first, then username
            Optional<User> userOpt = userRepository.findByEmail(identifier);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(identifier);
            }
            
            User user = userOpt.orElseThrow(() -> new RuntimeException("User not found for identifier: " + identifier));

            UserInfo userInfo = new UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole()
            );

            return ResponseEntity.ok(userInfo);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(404).body(error);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
