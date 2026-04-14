package com.aimaster.service;

import com.aimaster.model.AgendaItem;
import com.aimaster.repository.AgendaItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class AgendaItemService {

    private final AgendaItemRepository repository;

    public List<AgendaItem> findAll() {
        return repository.findAllByOrderByCreatedAtAsc();
    }

    public long count() {
        return repository.count();
    }

    @Transactional
    public AgendaItem save(AgendaItem item) {
        return repository.save(item);
    }

    @Transactional
    public Optional<AgendaItem> update(Long id, AgendaItem updated) {
        return repository.findById(id).map(existing -> {
            existing.setType(updated.getType());
            existing.setName(updated.getName());
            existing.setIcon(updated.getIcon());
            existing.setColor(updated.getColor());
            existing.setDayOfWeek(updated.getDayOfWeek());
            existing.setTime(updated.getTime());
            existing.setFrequency(updated.getFrequency());
            existing.setDate(updated.getDate());
            existing.setLocation(updated.getLocation());
            existing.setDescription(updated.getDescription());
            existing.setHighlight(updated.isHighlight());
            return repository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }

    /**
     * Formata todos os itens da agenda em texto estruturado para injeção no contexto da IA.
     * Returns empty string if agenda is empty.
     */
    public String formatAgendaForAI() {
        List<AgendaItem> all = repository.findAllByOrderByCreatedAtAsc();
        if (all.isEmpty()) return "";

        var sb = new StringBuilder();
        sb.append("════════ AGENDA EVJ — ESTAÇÃO VIDA JOVEM ════════\n");
        sb.append("Informações atualizadas da programação da EVJ:\n\n");

        // Fixed schedule
        List<AgendaItem> fixed = all.stream().filter(i -> "fixed".equals(i.getType())).toList();
        if (!fixed.isEmpty()) {
            sb.append("📅 PROGRAMAÇÃO FIXA (SEMANAL/REGULAR):\n");
            for (AgendaItem item : fixed) {
                sb.append("• ").append(item.getName());
                if (item.getDayOfWeek() != null) {
                    String day = DayOfWeek.of(item.getDayOfWeek() == 0 ? 7 : item.getDayOfWeek())
                            .getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
                    sb.append(" — ").append(capitalize(day));
                }
                if (item.getTime() != null && !item.getTime().isBlank())
                    sb.append(" às ").append(item.getTime());
                if (item.getFrequency() != null && !item.getFrequency().isBlank())
                    sb.append(" (").append(item.getFrequency()).append(")");
                if (item.getLocation() != null && !item.getLocation().isBlank())
                    sb.append(" | Local: ").append(item.getLocation());
                if (item.getDescription() != null && !item.getDescription().isBlank())
                    sb.append("\n  ↳ ").append(item.getDescription());
                sb.append("\n");
            }
            sb.append("\n");
        }

        // Dated events — split into upcoming and past
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<AgendaItem> events = all.stream()
                .filter(i -> "event".equals(i.getType()) && i.getDate() != null)
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();

        List<AgendaItem> upcoming = events.stream().filter(e -> !e.getDate().isBefore(today)).toList();
        List<AgendaItem> past     = events.stream().filter(e -> e.getDate().isBefore(today)).toList();

        if (!upcoming.isEmpty()) {
            sb.append("🌟 PRÓXIMOS EVENTOS:\n");
            for (AgendaItem item : upcoming) {
                sb.append("• ").append(item.getName());
                sb.append(" — ").append(item.getDate().format(fmt));
                if (item.getTime() != null && !item.getTime().isBlank())
                    sb.append(" às ").append(item.getTime());
                if (item.getLocation() != null && !item.getLocation().isBlank())
                    sb.append(" | Local: ").append(item.getLocation());
                if (item.isHighlight()) sb.append(" ⭐ DESTAQUE");
                if (item.getDescription() != null && !item.getDescription().isBlank())
                    sb.append("\n  ↳ ").append(item.getDescription());
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!past.isEmpty()) {
            sb.append("📌 EVENTOS PASSADOS RECENTES:\n");
            // Show only the 5 most recent past events
            int start = Math.max(0, past.size() - 5);
            for (int i = past.size() - 1; i >= start; i--) {
                AgendaItem item = past.get(i);
                sb.append("• ").append(item.getName());
                sb.append(" — ").append(item.getDate().format(fmt));
                if (item.getDescription() != null && !item.getDescription().isBlank())
                    sb.append(" | ").append(item.getDescription());
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("════════════════════════════════════════\n");
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
