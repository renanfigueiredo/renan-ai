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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
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
            emailService.sendVerificationEmail(newUser);
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

    /** Verifica o e-mail do usuário a partir do token enviado no cadastro (sem e-mail extra). */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<AppUser> opt = userRepository.findByApprovalToken(token);
        if (opt.isEmpty()) return false;
        AppUser user = opt.get();
        user.setStatus(UserStatus.ACTIVE);
        user.setApprovalToken(null);
        userRepository.save(user);
        log.info("E-mail verificado, conta ativada: {}", user.getEmail());
        return true;
    }

    public Optional<AppUser> findById(Long id) {
        return userRepository.findById(id);
    }

    // ── Admin CRUD ───────────────────────────────────────────────────────────

    public List<AppUser> getAllUsers() {
        return userRepository.findAllNonGuests();
    }

    public java.util.Map<String, Long> getUserStats() {
        return java.util.Map.of(
            "total",   userRepository.countNonGuests(),
            "active",  userRepository.countNonGuestsByStatus(UserStatus.ACTIVE),
            "pending", userRepository.countNonGuestsByStatus(UserStatus.PENDING),
            "admin",   userRepository.countNonGuestsByRole(UserRole.ADMIN)
        );
    }

    @Transactional
    public AppUser adminCreateUser(String name, String email, String rawPassword,
                                   UserRole role, UserStatus status) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }
        AppUser user = AppUser.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .status(status)
                .build();
        log.info("Admin criou usuário: {} ({})", email, role);
        return userRepository.save(user);
    }

    @Transactional
    public AppUser adminUpdateUser(Long id, String name, String email,
                                   UserRole role, UserStatus status) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-mail já utilizado por outro usuário.");
        }
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        log.info("Admin atualizou usuário id={} email={}", id, email);
        return userRepository.save(user);
    }

    @Transactional
    public void adminChangePassword(Long id, String newRawPassword) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
        log.info("Admin alterou senha do usuário id={}", id);
    }

    @Transactional
    public void adminDeleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }
        userRepository.deleteById(id);
        log.info("Admin excluiu usuário id={}", id);
    }

    // ── Password reset ────────────────────────────────────────────────────────

    /**
     * Gera um token de reset de senha para o e-mail informado.
     *
     * Boas práticas seguidas:
     * - UUID criptograficamente aleatório enviado ao usuário (URL-safe)
     * - Apenas o SHA-256 do UUID é armazenado no banco (token não reversível)
     * - Expira em 1 hora
     * - Responde sempre com a mesma mensagem, não revelando se o e-mail existe
     */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getStatus() != UserStatus.ACTIVE) return; // ignora contas pendentes/rejeitadas

            String rawToken  = UUID.randomUUID().toString();   // enviado ao usuário no link
            String tokenHash = sha256Hex(rawToken);            // armazenado no banco

            user.setResetToken(tokenHash);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            try {
                emailService.sendPasswordReset(user, rawToken);
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de reset para {}", email, e);
            }
            log.info("Token de reset gerado para {}", email);
        });
    }

    /**
     * Valida o token e retorna o e-mail do usuário para pré-preencher o formulário.
     * Retorna empty se o token for inválido ou expirado.
     */
    public Optional<String> validateResetToken(String rawToken) {
        String tokenHash = sha256Hex(rawToken);
        return userRepository.findByResetToken(tokenHash)
                .filter(u -> u.getResetTokenExpiry() != null
                          && u.getResetTokenExpiry().isAfter(LocalDateTime.now()))
                .map(AppUser::getEmail);
    }

    /**
     * Aplica a nova senha, invalida o token e desbloqueia o acesso.
     * Lança IllegalArgumentException se o token for inválido/expirado.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = sha256Hex(rawToken);
        AppUser user = userRepository.findByResetToken(tokenHash)
                .filter(u -> u.getResetTokenExpiry() != null
                          && u.getResetTokenExpiry().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Link de reset inválido ou expirado."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);           // token de uso único — revogado imediatamente
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Senha redefinida com sucesso para {}", user.getEmail());
    }

    /** SHA-256 do token em hexadecimal minúsculo (64 chars). */
    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 indisponível", e);
        }
    }
}
