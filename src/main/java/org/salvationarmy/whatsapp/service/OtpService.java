package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.entity.OtpCode;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.OtpCodeRepository;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    @Value("${app.otp.rate-limit-seconds:30}")
    private int rateLimitSeconds = 30;

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.otp.dev-mode:true}")
    private boolean devMode;

    @Value("${spring.mail.enabled:false}")
    private boolean mailEnabled;
    
    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generate and send OTP to email
     */
    @Transactional
    public void sendOtp(String email) {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        // Validate email format
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Rate limiting: check if OTP was sent recently
        LocalDateTime since = LocalDateTime.now().minusSeconds(rateLimitSeconds);
        long recentOtpCount = otpCodeRepository.countByEmailAndCreatedAtAfter(email, since);
        if (recentOtpCount > 0) {
            throw new IllegalStateException("Please wait " + rateLimitSeconds + " seconds before requesting another OTP");
        }

        // Invalidate previous unverified OTPs so only the latest code works
        otpCodeRepository.deleteUnverifiedByEmail(email);

        // Generate OTP
        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        // Save OTP code
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail(email);
        otpCode.setOtpHash(otpHash);
        otpCode.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpCode.setAttempts(0);
        otpCode.setVerified(false);
        otpCodeRepository.save(otpCode);

        // Send email or log in dev mode
        boolean shouldSendEmail = mailEnabled && mailSender != null && 
                                  mailHost != null && !mailHost.trim().isEmpty();
        
        if (shouldSendEmail) {
            try {
                sendOtpEmail(email, otp);
                log.info("OTP email sent successfully to {}", email);
            } catch (Exception e) {
                log.warn("Failed to send OTP email to {}: {}. Falling back to dev mode.", email, e.getMessage());
                if (devMode) {
                    log.info("=== OTP FOR {}: {} ===", email, otp);
                    log.info("(Email sending failed. OTP logged above for dev/testing.)");
                } else {
                    log.error("OTP email failed and dev-mode is off; not logging OTP. Fix SMTP or set OTP_DEV_MODE=true temporarily.");
                }
            }
        } else if (devMode) {
            log.warn("SMTP disabled or not configured (spring.mail.enabled / spring.mail.host). OTP not emailed — check server logs below.");
            log.info("=== OTP FOR {}: {} ===", email, otp);
            log.info("(Dev mode: set MAIL_ENABLED=true, MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD on the server to send real email.)");
        } else {
            throw new IllegalStateException(
                    "OTP email is not configured. Set environment variables MAIL_ENABLED=true, MAIL_HOST, MAIL_USERNAME, "
                            + "MAIL_PASSWORD, and MAIL_FROM (or enable OTP_DEV_MODE=true only for development).");
        }
    }

    /**
     * Verify OTP and return user (create if doesn't exist)
     */
    @Transactional
    public User verifyOtp(String email, String otp) {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (otp != null) {
            otp = otp.trim();
        }
        // Find latest unverified OTP (email is normalized so send and verify match)
        Optional<OtpCode> otpCodeOpt = otpCodeRepository.findFirstByEmailAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                email, LocalDateTime.now());

        if (otpCodeOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        OtpCode otpCode = otpCodeOpt.get();

        // Check attempts
        if (otpCode.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalStateException("Maximum verification attempts exceeded");
        }

        // Check expiry
        if (otpCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }

        // Verify OTP
        if (!passwordEncoder.matches(otp, otpCode.getOtpHash())) {
            otpCode.setAttempts(otpCode.getAttempts() + 1);
            otpCodeRepository.save(otpCode);
            throw new IllegalArgumentException("Invalid OTP");
        }

        // Mark as verified
        otpCode.setVerified(true);
        otpCodeRepository.save(otpCode);

        // Get or create user (case-insensitive lookup for existing users)
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
        } else {
            // Create new user
            user = new User();
            user.setEmail(email);
            user.setFullName(extractNameFromEmail(email));
            user.setRole("VIEWER");
            user.setStatus("ACTIVE");
        }
        user = userRepository.save(user);

        return user;
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private void sendOtpEmail(String email, String otp) {
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender is not configured");
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String from = mailFrom != null && !mailFrom.trim().isEmpty()
                    ? mailFrom.trim()
                    : (mailUsername != null && !mailUsername.trim().isEmpty() ? mailUsername.trim() : null);
            if (from != null) {
                message.setFrom(from);
            }
            message.setTo(email);
            message.setSubject("HTF Data collection - OTP Code");
            message.setText(String.format(
                    "Your OTP code is: %s\n\n" +
                    "This code will expire in %d minutes.\n\n" +
                    "If you didn't request this code, please ignore this email.",
                    otp, OTP_EXPIRY_MINUTES));
            mailSender.send(message);
            log.info("OTP email sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private String extractNameFromEmail(String email) {
        String localPart = email.split("@")[0];
        return localPart.substring(0, 1).toUpperCase() + localPart.substring(1);
    }

    @Transactional
    public void cleanupExpiredOtps() {
        otpCodeRepository.deleteExpiredCodes(LocalDateTime.now());
    }
}
