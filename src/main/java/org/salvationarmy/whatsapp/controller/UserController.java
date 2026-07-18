package org.salvationarmy.whatsapp.controller;

import org.salvationarmy.whatsapp.dto.UserInfo;
import org.salvationarmy.whatsapp.dto.UserResponse;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.salvationarmy.whatsapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<List<UserResponse>> getAllUsers(Authentication authentication) {
        try {
            List<UserResponse> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
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
