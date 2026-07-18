package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.dto.LoginRequest;
import org.salvationarmy.whatsapp.dto.LoginResponse;
import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.salvationarmy.whatsapp.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    public LoginResponse authenticate(LoginRequest request) {
        String identifier = request.getEmail() == null ? "" : request.getEmail().trim();
        User user = userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> userRepository.findByUsernameIgnoreCase(identifier))
                .orElseThrow(() -> new RuntimeException("Invalid username/email or password"));

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("No password set. Use OTP login or create a password first.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username/email or password");
        }

        String token = jwtUtil.generateTokenForEmail(user.getEmail());
        return new LoginResponse(token, user.getEmail());
    }
}






