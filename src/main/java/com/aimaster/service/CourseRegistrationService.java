package com.aimaster.service;

import com.aimaster.model.CourseRegistration;
import com.aimaster.model.CourseRegistrationStatus;
import com.aimaster.repository.CourseRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseRegistrationService {

    private final CourseRegistrationRepository repository;

    /** Registra um inscrito. Lança exceção se o e-mail já estiver inscrito neste curso. */
    @Transactional
    public CourseRegistration register(String courseKey, String name, String email, String phone) {
        if (repository.existsByCourseKeyAndEmail(courseKey, email)) {
            throw new IllegalArgumentException("Este e-mail já está inscrito neste curso.");
        }
        CourseRegistration reg = CourseRegistration.builder()
                .courseKey(courseKey)
                .name(name)
                .email(email.toLowerCase().strip())
                .phone(phone)
                .build();
        CourseRegistration saved = repository.save(reg);
        log.info("Nova inscrição no curso '{}': {} <{}>", courseKey, name, email);
        return saved;
    }

    /** Lista todas as inscrições de um curso. */
    public List<CourseRegistration> listByCourse(String courseKey) {
        return repository.findByCourseKeyOrderByCreatedAtDesc(courseKey);
    }

    /** Estatísticas de um curso. */
    public Map<String, Long> getStats(String courseKey) {
        long total      = repository.countByCourseKey(courseKey);
        long confirmed  = repository.countByCourseKeyAndStatus(courseKey, CourseRegistrationStatus.CONFIRMED);
        long cancelled  = repository.countByCourseKeyAndStatus(courseKey, CourseRegistrationStatus.CANCELLED);
        long registered = repository.countByCourseKeyAndStatus(courseKey, CourseRegistrationStatus.REGISTERED);
        return Map.of("total", total, "confirmed", confirmed,
                      "cancelled", cancelled, "registered", registered);
    }

    /** Atualiza status e/ou observações admin de uma inscrição. */
    @Transactional
    public CourseRegistration updateRegistration(Long id, CourseRegistrationStatus status, String notes) {
        CourseRegistration reg = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscrição não encontrada: " + id));
        if (status != null) reg.setStatus(status);
        if (notes  != null) reg.setNotes(notes);
        return repository.save(reg);
    }

    /** Remove uma inscrição. */
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Inscrição não encontrada: " + id);
        }
        repository.deleteById(id);
        log.info("Inscrição {} removida pelo admin.", id);
    }

    public Optional<CourseRegistration> findById(Long id) {
        return repository.findById(id);
    }
}
