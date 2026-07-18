package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.dto.CapturePaymentRequest;
import org.salvationarmy.whatsapp.dto.PaymentResponse;
import org.salvationarmy.whatsapp.entity.*;
import org.salvationarmy.whatsapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContributionCategoryRepository categoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Capture a new payment
     */
    @Transactional
    public PaymentResponse capturePayment(CapturePaymentRequest request, UUID recordedByUserId) {
        // Validate member
        User member = userRepository.findById(request.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Validate category
        ContributionCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Validate project if provided
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            
            // Validate category type matches
            if (category.getType() != ContributionCategory.CategoryType.PROJECT) {
                throw new RuntimeException("Category type must be PROJECT when project is specified");
            }
        }

        // Validate event if provided
        if (request.getEventId() != null) {
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            
            // Validate category type matches
            if (category.getType() != ContributionCategory.CategoryType.EVENT) {
                throw new RuntimeException("Category type must be EVENT when event is specified");
            }
        }

        // Create payment
        Payment payment = new Payment();
        payment.setMemberId(request.getMemberId());
        payment.setCategoryId(request.getCategoryId());
        payment.setProjectId(request.getProjectId());
        payment.setEventId(request.getEventId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setNotes(request.getNotes());
        payment.setRecordedByUserId(recordedByUserId);
        payment.setRecordedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        // Update project collected amount if applicable
        if (project != null) {
            BigDecimal currentCollected = project.getCollectedAmount() != null ? project.getCollectedAmount() : BigDecimal.ZERO;
            project.setCollectedAmount(currentCollected.add(request.getAmount()));
            projectRepository.save(project);
        }

        // Create notification for member
        String notificationTitle = "Payment Received";
        String notificationMessage = String.format(
            "Hi %s, your payment of %s %s for %s was recorded on %s. %s",
            member.getFullName() != null ? member.getFullName() : member.getEmail(),
            request.getCurrency(),
            request.getAmount(),
            category.getName(),
            LocalDateTime.now().toLocalDate(),
            request.getReferenceNumber() != null ? "Ref: " + request.getReferenceNumber() : ""
        );

        notificationService.createNotification(
            request.getMemberId(),
            notificationTitle,
            notificationMessage,
            Notification.NotificationChannel.IN_APP,
            saved.getId()
        );

        // Also send email notification
        notificationService.createNotification(
            request.getMemberId(),
            notificationTitle,
            notificationMessage,
            Notification.NotificationChannel.EMAIL,
            saved.getId()
        );

        log.info("Payment captured: paymentId={}, memberId={}, amount={}, category={}", 
                saved.getId(), request.getMemberId(), request.getAmount(), category.getName());

        return toResponse(saved);
    }

    /**
     * Get payments with pagination
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(this::toResponse);
    }

    /**
     * Get payments by member
     */
    public Page<PaymentResponse> getPaymentsByMember(UUID memberId, Pageable pageable) {
        return paymentRepository.findByMemberIdOrderByRecordedAtDesc(memberId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get payments by category
     */
    public Page<PaymentResponse> getPaymentsByCategory(UUID categoryId, Pageable pageable) {
        return paymentRepository.findByCategoryIdOrderByRecordedAtDesc(categoryId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get payments by project
     */
    public Page<PaymentResponse> getPaymentsByProject(UUID projectId, Pageable pageable) {
        return paymentRepository.findByProjectIdOrderByRecordedAtDesc(projectId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get payments by event
     */
    public Page<PaymentResponse> getPaymentsByEvent(UUID eventId, Pageable pageable) {
        return paymentRepository.findByEventIdOrderByRecordedAtDesc(eventId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get payment by ID
     */
    public PaymentResponse getPaymentById(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .memberId(payment.getMemberId())
                .categoryId(payment.getCategoryId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .recordedByUserId(payment.getRecordedByUserId())
                .recordedAt(payment.getRecordedAt())
                .createdAt(payment.getCreatedAt());

        // Always resolve by FK id — lazy ManyToOne proxies are non-null before load, so
        // "if (payment.getMember() != null)" wrongly skips the repository path and triggers LazyInitializationException.
        userRepository.findById(payment.getMemberId()).ifPresent(member -> {
            builder.memberName(member.getFullName());
            builder.memberEmail(member.getEmail());
        });

        categoryRepository.findById(payment.getCategoryId()).ifPresent(category -> {
            builder.categoryName(category.getName());
            if (category.getType() != null) {
                builder.categoryType(category.getType().name());
            }
        });

        if (payment.getProjectId() != null) {
            projectRepository.findById(payment.getProjectId())
                    .ifPresent(project -> builder.projectName(project.getName()));
        }

        if (payment.getEventId() != null) {
            eventRepository.findById(payment.getEventId())
                    .ifPresent(event -> builder.eventName(event.getName()));
        }

        userRepository.findById(payment.getRecordedByUserId())
                .ifPresent(user -> builder.recordedByName(user.getFullName()));

        return builder.build();
    }
}
