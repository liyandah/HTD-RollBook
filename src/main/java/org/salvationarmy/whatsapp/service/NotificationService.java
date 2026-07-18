package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.dto.NotificationResponse;
import org.salvationarmy.whatsapp.entity.Notification;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.NotificationRepository;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private OtpService otpService; // For email notifications

    /**
     * Create and send notification
     */
    @Transactional
    public Notification createNotification(UUID userId, String title, String message, 
                                          Notification.NotificationChannel channel, UUID relatedPaymentId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setChannel(channel);
        notification.setRelatedPaymentId(relatedPaymentId);
        notification.setIsRead(false);
        
        Notification saved = notificationRepository.save(notification);
        
        // Send email if channel is EMAIL
        if (channel == Notification.NotificationChannel.EMAIL) {
            sendEmailNotification(userId, title, message);
        }
        
        log.info("Notification created: userId={}, title={}, channel={}", userId, title, channel);
        return saved;
    }

    /**
     * Get notifications for user
     */
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get unread notifications count
     */
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Get unread notifications
     */
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        notificationRepository.markAsRead(notificationId, LocalDateTime.now());
        log.info("Notification marked as read: notificationId={}, userId={}", notificationId, userId);
    }

    /**
     * Mark all notifications as read for user
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
        log.info("All notifications marked as read for userId={}", userId);
    }

    private void sendEmailNotification(UUID userId, String title, String message) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Use OtpService's email sending capability if available
            // For now, just log - can be enhanced later with proper email service
            log.info("Email notification would be sent to {}: {} - {}", user.getEmail(), title, message);
        } catch (Exception e) {
            log.error("Failed to send email notification to userId={}: {}", userId, e.getMessage());
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .channel(notification.getChannel().name())
                .isRead(notification.getIsRead())
                .relatedPaymentId(notification.getRelatedPaymentId())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
