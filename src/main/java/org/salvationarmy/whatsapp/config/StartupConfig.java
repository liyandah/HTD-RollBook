package org.salvationarmy.whatsapp.config;

import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.util.Optional;

/**
 * Configuration that runs on application startup
 */
@Configuration
public class StartupConfig {

    private static final Logger logger = LoggerFactory.getLogger(StartupConfig.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    /**
     * Ensure the uploads directory exists on startup
     */
    @Bean
    public CommandLineRunner createUploadDirectory() {
        return args -> {
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    logger.info("✅ Created uploads directory: {}", directory.getAbsolutePath());
                } else {
                    logger.warn("⚠️ Failed to create uploads directory: {}", directory.getAbsolutePath());
                }
            } else {
                logger.info("✅ Uploads directory already exists: {}", directory.getAbsolutePath());
            }
        };
    }

    /**
     * Ensure default admin exists and reset password on startup.
     */
    @Bean
    public CommandLineRunner ensureDefaultAdminUser() {
        return args -> {
            String normalizedUsername = adminUsername == null || adminUsername.isBlank() ? "admin" : adminUsername.trim();
            String normalizedEmail = normalizedUsername + "@salvationarmy.org";

            Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(normalizedUsername)
                    .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail));

            User user = userOpt.orElseGet(User::new);
            user.setUsername(normalizedUsername);
            user.setEmail(normalizedEmail);
            user.setFullName("System Administrator");
            user.setRole("ADMIN");
            user.setStatus("ACTIVE");
            user.setPassword(passwordEncoder.encode(adminPassword));

            userRepository.save(user);
            logger.info("✅ Default admin user ensured. Login with username '{}' and configured admin password.", normalizedUsername);
        };
    }
}





