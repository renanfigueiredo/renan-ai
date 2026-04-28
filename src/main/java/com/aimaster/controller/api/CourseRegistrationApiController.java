package com.aimaster.controller.api;

import com.aimaster.model.CourseRegistration;
import com.aimaster.model.CourseRegistrationStatus;
import com.aimaster.service.CourseRegistrationService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints para inscrições no curso "Namoro com Propósito".
 *
 * Públicos:
 *   POST /api/course/namoro/register  — inscrição pública
 *
 * Admin (ROLE_ADMIN):
 *   GET    /api/admin/course/namoro              — lista inscritos
 *   GET    /api/admin/course/namoro/stats        — estatísticas
 *   PUT    /api/admin/course/namoro/{id}         — atualizar status/notas
 *   DELETE /api/admin/course/namoro/{id}         — remover inscrição
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CourseRegistrationApiController {

    private static final String COURSE_KEY = "namoro-com-proposito";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CourseRegistrationService service;

    // ── DTOs ──────────────────────────────────────────────────────────────

    record RegisterRequest(
            @NotBlank(message = "Nome é obrigatório") String name,
            @Email(message = "E-mail inválido") @NotBlank(message = "E-mail é obrigatório") String email,
            String phone) {}

    record UpdateRequest(String status, String notes) {}

    // ── Helper ────────────────────────────────────────────────────────────

    private Map<String, Object> toMap(CourseRegistration r) {
        return Map.of(
                "id",        r.getId(),
                "name",      r.getName(),
                "email",     r.getEmail(),
                "phone",     r.getPhone() != null ? r.getPhone() : "",
                "status",    r.getStatus().name(),
                "notes",     r.getNotes() != null ? r.getNotes() : "",
                "createdAt", r.getCreatedAt() != null ? r.getCreatedAt().format(FMT) : ""
        );
    }

    // ── PUBLIC: inscrição ────────────────────────────────────────────────

    @PostMapping("/api/course/namoro/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            CourseRegistration reg = service.register(COURSE_KEY, req.name(), req.email(), req.phone());
            return ResponseEntity.ok(Map.of(
                    "message", "Inscrição realizada com sucesso!",
                    "id", reg.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── ADMIN: listar ─────────────────────────────────────────────────────

    @GetMapping("/api/admin/course/namoro")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(
                service.listByCourse(COURSE_KEY).stream().map(this::toMap).toList()
        );
    }

    // ── ADMIN: estatísticas ───────────────────────────────────────────────

    @GetMapping("/api/admin/course/namoro/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(service.getStats(COURSE_KEY));
    }

    // ── ADMIN: atualizar ──────────────────────────────────────────────────

    @PutMapping("/api/admin/course/namoro/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateRequest req) {
        try {
            CourseRegistrationStatus status = null;
            if (req.status() != null && !req.status().isBlank()) {
                status = CourseRegistrationStatus.valueOf(req.status().toUpperCase());
            }
            CourseRegistration updated = service.updateRegistration(id, status, req.notes());
            return ResponseEntity.ok(toMap(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── ADMIN: excluir ────────────────────────────────────────────────────

    @DeleteMapping("/api/admin/course/namoro/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
