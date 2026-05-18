package com.aimaster.config;

import com.aimaster.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final GuestAuthFilter guestAuthFilter;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(guestAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Public routes
                .requestMatchers("/", "/login", "/register", "/pending", "/verify-email",
                                 "/admin/approve/**", "/admin/reject/**",
                                 "/forgot-password", "/reset-password",
                                 "/guest/**",
                                 "/portal", "/portal/**",
                                 "/api/portal/chat",
                                 "/api/me",
                                 "/api/agenda",
                                 "/api/v1/**",
                                 "/api/course/namoro/register",
                                 "/swagger-ui/**", "/swagger-ui.html",
                                 "/v3/api-docs/**",
                                 "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Admin-only routes
                .requestMatchers("/admin/users/**", "/admin/course/**", "/api/admin/**").hasRole("ADMIN")
                // Everything else requires authentication (includes ROLE_GUEST)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            // Keep CSRF enabled but exempt the API endpoints (called via fetch/AJAX)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/ws/**", "/guest/**")
            );
        return http.build();
    }
}
