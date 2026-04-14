package com.aimaster.controller.api;

import com.aimaster.model.AgendaItem;
import com.aimaster.service.AgendaItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AgendaApiController {

    private final AgendaItemService agendaItemService;

    record AgendaRequest(
            String type, String name, String icon, String color,
            Integer dayOfWeek, String time, String frequency,
            String date, String location, String description, boolean highlight
    ) {}

    /** Public — qualquer visitante pode ler a agenda */
    @GetMapping("/api/agenda")
    public List<AgendaItem> getAll() {
        return agendaItemService.findAll();
    }

    /** Admin-only — criação via /api/admin/agenda (protegido pela SecurityConfig) */
    @PostMapping("/api/admin/agenda")
    public ResponseEntity<AgendaItem> create(@RequestBody AgendaRequest req) {
        return ResponseEntity.ok(agendaItemService.save(toEntity(req)));
    }

    /** Admin-only — atualização */
    @PutMapping("/api/admin/agenda/{id}")
    public ResponseEntity<AgendaItem> update(@PathVariable Long id, @RequestBody AgendaRequest req) {
        return agendaItemService.update(id, toEntity(req))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Admin-only — exclusão */
    @DeleteMapping("/api/admin/agenda/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agendaItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AgendaItem toEntity(AgendaRequest req) {
        return AgendaItem.builder()
                .type(req.type())
                .name(req.name())
                .icon(req.icon() != null && !req.icon().isBlank() ? req.icon() : "calendar-heart")
                .color(req.color() != null && !req.color().isBlank() ? req.color() : "cyan")
                .dayOfWeek(req.dayOfWeek())
                .time(req.time())
                .frequency(req.frequency())
                .date(req.date() != null && !req.date().isBlank() ? LocalDate.parse(req.date()) : null)
                .location(req.location())
                .description(req.description())
                .highlight(req.highlight())
                .build();
    }
}
