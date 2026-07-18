package org.salvationarmy.whatsapp.controller;

import jakarta.validation.Valid;
import org.salvationarmy.whatsapp.dto.EventRequest;
import org.salvationarmy.whatsapp.dto.EventResponse;
import org.salvationarmy.whatsapp.service.EventService;
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
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

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
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/active")
    public ResponseEntity<List<EventResponse>> getActiveEvents() {
        return ResponseEntity.ok(eventService.getActiveEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEvent(@PathVariable UUID id) {
        try {
            EventResponse event = eventService.getEventById(id);
            return ResponseEntity.ok(event);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Event not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping
    public ResponseEntity<?> createEvent(
            @Valid @RequestBody EventRequest request,
            Authentication authentication) {
        try {
            UUID createdByUserId = getCurrentUserId(authentication);
            
            // Check if user has permission (ADMIN only)
            org.salvationarmy.whatsapp.entity.User user = userRepository.findById(createdByUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!user.getRole().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Only ADMIN can create events");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            EventResponse response = eventService.createEvent(request, createdByUserId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create event");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventRequest request,
            Authentication authentication) {
        try {
            UUID userId = getCurrentUserId(authentication);
            
            // Check if user has permission (ADMIN only)
            org.salvationarmy.whatsapp.entity.User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!user.getRole().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Only ADMIN can update events");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            EventResponse response = eventService.updateEvent(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update event");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(
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
                error.put("message", "Only ADMIN can delete events");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            eventService.deleteEvent(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Event deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete event");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
