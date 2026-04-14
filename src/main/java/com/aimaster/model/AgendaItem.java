package com.aimaster.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agenda_items")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AgendaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "fixed" = programação fixa semanal/mensal, "event" = evento datado */
    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false)
    private String name;

    @Builder.Default private String icon  = "calendar-heart";
    @Builder.Default private String color = "cyan";

    // ── Fixed-schedule fields ─────────────────────────────────────
    private Integer dayOfWeek; // 0 = Domingo … 6 = Sábado
    private String  time;      // HH:mm
    private String  frequency; // "Toda semana", "Todo domingo", etc.

    // ── Dated-event fields ────────────────────────────────────────
    private LocalDate date;

    // ── Common ───────────────────────────────────────────────────
    private String location;

    @Column(length = 2000)
    private String description;

    @Builder.Default private boolean highlight = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
