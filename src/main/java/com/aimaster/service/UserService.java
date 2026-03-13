package com.aimaster.service;

import com.aimaster.model.AppUser;
import com.aimaster.model.UserRole;
import com.aimaster.model.UserStatus;
import com.aimaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        if (user.getStatus() == UserStatus.PENDING) {
            throw new UsernameNotFoundException("Conta pendente de aprovação. Aguarde o e-mail de confirmação.");
        }
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new UsernameNotFoundException("Acesso negado. Sua solicitação foi recusada.");
        }

        return User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }

    @Transactional
    public AppUser register(String name, String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        String token = UUID.randomUUID().toString();

        AppUser newUser = AppUser.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(UserRole.USER)
                .status(UserStatus.PENDING)
                .approvalToken(token)
                .build();

        userRepository.save(newUser);

        try {
            emailService.sendApprovalRequest(newUser);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de aprovação para novo usuário: {}", email, e);
        }

        log.info("Novo usuário registrado e aguardando aprovação: {}", email);
        return newUser;
    }

    @Transactional
    public boolean approveUser(String token) {
        Optional<AppUser> opt = userRepository.findByApprovalToken(token);
        if (opt.isEmpty()) return false;

        AppUser user = opt.get();
        user.setStatus(UserStatus.ACTIVE);
        user.setApprovalToken(null);
        userRepository.save(user);

        try {
            emailService.sendAccountApproved(user);
        } catch (Exception e) {
            log.warn("Falha ao notificar usuário da aprovação: {}", user.getEmail(), e);
        }

        log.info("Usuário aprovado: {}", user.getEmail());
        return true;
    }

    @Transactional
    public boolean rejectUser(String token) {
        Optional<AppUser> opt = userRepository.findByApprovalToken(token);
        if (opt.isEmpty()) return false;

        AppUser user = opt.get();
        user.setStatus(UserStatus.REJECTED);
        user.setApprovalToken(null);
        userRepository.save(user);

        log.info("Usuário rejeitado: {}", user.getEmail());
        return true;
    }

    public Optional<AppUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
