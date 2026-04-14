package com.aimaster.controller.api;

import com.aimaster.model.AppUser;
import com.aimaster.model.UserRole;
import com.aimaster.model.UserStatus;
import com.aimaster.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * REST API for admin user management.
 * All endpoints require ROLE_ADMIN (enforced in SecurityConfig + method annotation).
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminApiController {

    private final UserService userService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── DTOs ─────────────────────────────────────────────────────────────
    record CreateUserRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @Size(min = 8) @NotBlank String password,
            String role,
            String status) {}

    record UpdateUserRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            String role,
            String status) {}

    record ChangePasswordRequest(@Size(min = 8) @NotBlank String password) {}

    // ── Helper ────────────────────────────────────────────────────────────
    private Map<String, Object> toMap(AppUser u) {
        return Map.of(
                "id",        u.getId(),
                "name",      u.getName(),
                "email",     u.getEmail(),
                "role",      u.getRole().name(),
                "status",    u.getStatus().name(),
                "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().format(FMT) : ""
        );
    }

    // ── List ──────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        return ResponseEntity.ok(userService.getAllUsers().stream().map(this::toMap).toList());
    }

    // ── Stats ─────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(userService.getUserStats());
    }

    // ── Create ────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest req) {
        try {
            UserRole role   = parseRole(req.role());
            UserStatus status = parseStatus(req.status());
            AppUser created = userService.adminCreateUser(req.name(), req.email(), req.password(), role, status);
            return ResponseEntity.ok(toMap(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Update ────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody UpdateUserRequest req,
                                        Principal principal) {
        try {
            // Prevent self-demotion from ADMIN
            AppUser current = userService.findByEmail(principal.getName()).orElseThrow();
            if (current.getId().equals(id) && !"ADMIN".equalsIgnoreCase(req.role())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Você não pode remover sua própria role de administrador."));
            }
            UserRole role     = parseRole(req.role());
            UserStatus status = parseStatus(req.status());
            AppUser updated   = userService.adminUpdateUser(id, req.name(), req.email(), role, status);
            return ResponseEntity.ok(toMap(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Change Password ───────────────────────────────────────────────────
    @PatchMapping("/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable Long id,
                                            @RequestBody ChangePasswordRequest req) {
        try {
            userService.adminChangePassword(id, req.password());
            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Principal principal) {
        try {
            // Prevent self-deletion
            AppUser current = userService.findByEmail(principal.getName()).orElseThrow();
            if (current.getId().equals(id)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Você não pode excluir sua própria conta pelo painel."));
            }
            // Prevent deleting last admin
            long adminCount = userService.getUserStats().get("admin");
            AppUser target  = userService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
            if (target.getRole() == UserRole.ADMIN && adminCount <= 1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Não é possível excluir o último administrador do sistema."));
            }
            userService.adminDeleteUser(id);
            return ResponseEntity.ok(Map.of("message", "Usuário excluído."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private UserRole parseRole(String role) {
        try { return UserRole.valueOf(role.toUpperCase()); }
        catch (Exception e) { return UserRole.USER; }
    }

    private UserStatus parseStatus(String status) {
        try { return UserStatus.valueOf(status.toUpperCase()); }
        catch (Exception e) { return UserStatus.ACTIVE; }
    }
}
