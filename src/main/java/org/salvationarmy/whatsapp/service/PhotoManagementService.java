package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.dto.PhotoRejectRequest;
import org.salvationarmy.whatsapp.dto.PhotoRequestRequest;
import org.salvationarmy.whatsapp.dto.PhotoStatusResponse;
import org.salvationarmy.whatsapp.entity.Notification;
import org.salvationarmy.whatsapp.entity.SoldierRecord;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.NotificationRepository;
import org.salvationarmy.whatsapp.repository.SoldierRecordRepository;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PhotoManagementService {
    
    private static final Logger log = LoggerFactory.getLogger(PhotoManagementService.class);
    
    @Autowired
    private SoldierRecordRepository soldierRecordRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Request photo upload/resubmission from a user
     */
    @Transactional
    public void requestPhoto(UUID recordId, UUID adminUserId, PhotoRequestRequest request) {
        SoldierRecord record = soldierRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        
        // Verify admin exists (but don't store in variable to avoid unused warning)
        userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
        
        // Update record
        record.setPhotoStatus(SoldierRecord.PhotoStatus.RESUBMIT_REQUESTED);
        record.setPhotoRequestedAt(LocalDateTime.now());
        record.setPhotoRequestedBy(adminUserId);
        record.setPhotoReviewNotes(request.getNotes());
        soldierRecordRepository.save(record);
        
        log.info("[PhotoManagement] Photo requested: recordId={}, adminId={}, reason={}", 
                recordId, adminUserId, request.getReason());
        
        // Create notification for user
        if (record.getUserId() != null) {
            String message = buildPhotoRequestMessage(request.getReason(), request.getNotes());
            notificationService.createNotification(
                    record.getUserId(),
                    "Photo Upload Required",
                    message,
                    Notification.NotificationChannel.IN_APP,
                    null
            );
            
            log.info("[PhotoManagement] Notification created for userId={}", record.getUserId());
        }
        
        // Send system message to user's bot conversation if exists
        sendPhotoRequestToChat(record, request);
    }
    
    /**
     * Approve a user's photo
     */
    @Transactional
    public void approvePhoto(UUID recordId, UUID adminUserId) {
        SoldierRecord record = soldierRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        
        if (record.getPersonImagePath() == null || record.getPersonImagePath().isEmpty()) {
            throw new RuntimeException("Cannot approve: no photo uploaded");
        }
        
        record.setPhotoStatus(SoldierRecord.PhotoStatus.APPROVED);
        record.setPhotoReviewedAt(LocalDateTime.now());
        soldierRecordRepository.save(record);
        
        log.info("[PhotoManagement] Photo approved: recordId={}, adminId={}", recordId, adminUserId);
        
        // Create notification for user
        if (record.getUserId() != null) {
            notificationService.createNotification(
                    record.getUserId(),
                    "Photo Approved",
                    "Your personal photo has been approved by an administrator.",
                    Notification.NotificationChannel.IN_APP,
                    null
            );
        }
    }
    
    /**
     * Reject a user's photo
     */
    @Transactional
    public void rejectPhoto(UUID recordId, UUID adminUserId, PhotoRejectRequest request) {
        SoldierRecord record = soldierRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        
        record.setPhotoStatus(SoldierRecord.PhotoStatus.REJECTED);
        record.setPhotoReviewedAt(LocalDateTime.now());
        record.setPhotoReviewNotes(request.getNotes());
        // Also set as resubmit requested so user knows to resend
        record.setPhotoRequestedAt(LocalDateTime.now());
        record.setPhotoRequestedBy(adminUserId);
        soldierRecordRepository.save(record);
        
        log.info("[PhotoManagement] Photo rejected: recordId={}, adminId={}, notes={}", 
                recordId, adminUserId, request.getNotes());
        
        // Create notification for user
        if (record.getUserId() != null) {
            String message = "Your photo was rejected. Reason: " + request.getNotes() + 
                    "\n\nPlease upload a new photo.";
            notificationService.createNotification(
                    record.getUserId(),
                    "Photo Rejected",
                    message,
                    Notification.NotificationChannel.IN_APP,
                    null
            );
        }
        
        // Send system message to user's bot conversation
        sendPhotoRejectionToChat(record, request.getNotes());
    }
    
    /**
     * Get photo status for a user
     */
    public PhotoStatusResponse getPhotoStatus(UUID userId) {
        java.util.List<SoldierRecord> records = soldierRecordRepository.findByUserId(userId);
        Optional<SoldierRecord> recordOpt = records.stream().findFirst();
        
        if (recordOpt.isEmpty()) {
            // No record yet - return MISSING status
            PhotoStatusResponse response = new PhotoStatusResponse();
            response.setStatus(SoldierRecord.PhotoStatus.MISSING);
            response.setRequiresAction(false);
            return response;
        }
        
        SoldierRecord record = recordOpt.get();
        PhotoStatusResponse response = new PhotoStatusResponse();
        response.setStatus(record.getPhotoStatus() != null ? record.getPhotoStatus() : SoldierRecord.PhotoStatus.MISSING);
        response.setPhotoUrl(record.getPersonImagePath());
        response.setRequestedAt(record.getPhotoRequestedAt());
        response.setReviewedAt(record.getPhotoReviewedAt());
        response.setReviewNotes(record.getPhotoReviewNotes());
        response.setRequestedByUserId(record.getPhotoRequestedBy());
        
        if (record.getPhotoRequestedBy() != null) {
            userRepository.findById(record.getPhotoRequestedBy())
                    .ifPresent(admin -> response.setRequestedByUserName(admin.getFullName()));
        }
        
        // Determine if action is required
        response.setRequiresAction(
                response.getStatus() == SoldierRecord.PhotoStatus.MISSING ||
                response.getStatus() == SoldierRecord.PhotoStatus.RESUBMIT_REQUESTED ||
                response.getStatus() == SoldierRecord.PhotoStatus.REJECTED
        );
        
        return response;
    }
    
    /**
     * Update photo status when user uploads a photo
     */
    @Transactional
    public void onPhotoUploaded(UUID userId, String photoPath) {
        java.util.List<SoldierRecord> records = soldierRecordRepository.findByUserId(userId);
        Optional<SoldierRecord> recordOpt = records.stream().findFirst();
        
        if (recordOpt.isPresent()) {
            SoldierRecord record = recordOpt.get();
            record.setPersonImagePath(photoPath);
            record.setPhotoStatus(SoldierRecord.PhotoStatus.UPLOADED);
            soldierRecordRepository.save(record);
            
            log.info("[PhotoManagement] Photo uploaded: userId={}, photoPath={}", userId, photoPath);
            
            // Notify admins that a new photo needs review
            notifyAdminsOfNewPhoto(record);
        }
    }
    
    private String buildPhotoRequestMessage(String reason, String notes) {
        StringBuilder message = new StringBuilder();
        message.append("An administrator has requested that you upload or resubmit your personal photo.");
        
        if (reason != null && !reason.isEmpty()) {
            message.append("\n\nReason: ");
            switch (reason.toUpperCase()) {
                case "MISSING":
                    message.append("No photo uploaded");
                    break;
                case "UNCLEAR":
                    message.append("Photo is unclear");
                    break;
                case "WRONG_PERSON":
                    message.append("Wrong person in photo");
                    break;
                case "OTHER":
                    message.append("Other");
                    break;
                default:
                    message.append(reason);
            }
        }
        
        if (notes != null && !notes.isEmpty()) {
            message.append("\n\nNotes: ").append(notes);
        }
        
        message.append("\n\nPlease upload your photo using the chat bot or the upload button.");
        
        return message.toString();
    }
    
    private void sendPhotoRequestToChat(SoldierRecord record, PhotoRequestRequest request) {
        // This will be handled by BotRegistrationService when user next interacts
        // For now, we'll create a system message in the conversation
        // TODO: Implement system message sending
        log.info("[PhotoManagement] Photo request should be sent to chat for userId={}", record.getUserId());
    }
    
    private void sendPhotoRejectionToChat(SoldierRecord record, String notes) {
        // This will be handled by BotRegistrationService
        log.info("[PhotoManagement] Photo rejection should be sent to chat for userId={}", record.getUserId());
    }
    
    private void notifyAdminsOfNewPhoto(SoldierRecord record) {
        // Notify all admins that a new photo needs review
        userRepository.findByRole("ADMIN").forEach(admin -> {
            String message = String.format(
                    "User %s %s has uploaded a new photo for review.",
                    record.getFirstName() != null ? record.getFirstName() : "",
                    record.getFamilyName() != null ? record.getFamilyName() : ""
            );
            notificationService.createNotification(
                    admin.getId(),
                    "New Photo for Review",
                    message,
                    Notification.NotificationChannel.IN_APP,
                    null
            );
        });
        
        log.info("[PhotoManagement] Notified admins of new photo upload for recordId={}", record.getId());
    }
}
