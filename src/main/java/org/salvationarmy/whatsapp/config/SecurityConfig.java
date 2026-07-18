package org.salvationarmy.whatsapp.config;

import org.salvationarmy.whatsapp.entity.User;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.salvationarmy.whatsapp.security.JwtAuthenticationEntryPoint;
import org.salvationarmy.whatsapp.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Autowired
        private CorsConfigurationSource corsConfigurationSource;

        @Autowired
        private UserRepository userRepository;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /** Load user by email (Spring Security calls this loadUserByUsername; we use the argument as email for JWT subject). */
        @Bean
        public UserDetailsService userDetailsService() {
                return email -> {
                        User user = userRepository.findByEmailIgnoreCase(email)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
                        String password = user.getPassword() != null ? user.getPassword() : "";
                        return new org.springframework.security.core.userdetails.User(
                                user.getEmail(),
                                password,
                                new ArrayList<>());
                };
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(this.corsConfigurationSource))
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                // Allow OPTIONS requests for CORS preflight
                                                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                                                .permitAll()
                                                .requestMatchers("/api/whatsapp/**").permitAll()
                                                .requestMatchers("/", "/favicon.ico").permitAll()
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/api/bot/**").permitAll()
                                                .requestMatchers("/api/chat/**").permitAll()
                                                .requestMatchers("/webhooks/**").permitAll()
                                                .requestMatchers("/api/dialogflow/**").permitAll()
                                                .requestMatchers("/uploads/**").permitAll()
                                                .requestMatchers("/api/images/**").permitAll()
                                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**",
                                                                "/v3/api-docs/**")
                                                .permitAll()
                                                .requestMatchers("/actuator/**").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/records/register-bulk")
                                                .permitAll()
                                                .requestMatchers("/api/**").authenticated()
                                                .anyRequest().permitAll())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                                // Expired/missing JWT leaves the user anonymous; without this Spring
                                                // returns 403 and the SPA never clears the stale token (401 handler).
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        Authentication authentication = SecurityContextHolder
                                                                        .getContext().getAuthentication();
                                                        boolean anonymous = authentication == null
                                                                        || !authentication.isAuthenticated()
                                                                        || authentication instanceof AnonymousAuthenticationToken;
                                                        String path = request.getRequestURI();
                                                        if (anonymous && path != null && path.startsWith("/api/")) {
                                                                jwtAuthenticationEntryPoint.commence(request,
                                                                                response,
                                                                                new InsufficientAuthenticationException(
                                                                                                "Authentication required"));
                                                                return;
                                                        }
                                                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                                                }))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

                http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
