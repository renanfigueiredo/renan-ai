package com.aimaster.config;

import com.aimaster.model.AgendaItem;
import com.aimaster.model.AppUser;
import com.aimaster.model.UserRole;
import com.aimaster.model.UserStatus;
import com.aimaster.repository.UserRepository;
import com.aimaster.service.AgendaItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final AgendaItemService  agendaItemService;

    /**
     * Senha do admin inicial. Se não for fornecida via env (ADMIN_INITIAL_PASSWORD),
     * uma senha aleatória forte é gerada e logada uma única vez na criação.
     */
    @Value("${app.admin.initial-password:}")
    private String adminInitialPassword;

    @Value("${app.admin.email:admin@evj.app.br}")
    private String adminEmail;

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedAgenda();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        String rawPassword = (adminInitialPassword == null || adminInitialPassword.isBlank())
                ? generateRandomPassword()
                : adminInitialPassword;

        AppUser admin = AppUser.builder()
                .name("Administrador")
                .email(adminEmail)
                .password(passwordEncoder.encode(rawPassword))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(admin);

        if (adminInitialPassword == null || adminInitialPassword.isBlank()) {
            log.warn("=================================================================");
            log.warn("ADMIN inicial criado com SENHA ALEATÓRIA — anote agora:");
            log.warn("  email   : {}", adminEmail);
            log.warn("  senha   : {}", rawPassword);
            log.warn("Defina ADMIN_INITIAL_PASSWORD na próxima inicialização para evitar regeração.");
            log.warn("=================================================================");
        } else {
            log.info("Usuário administrador criado: {} (senha definida via ADMIN_INITIAL_PASSWORD)", adminEmail);
        }
    }

    private static String generateRandomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void seedAgenda() {
        if (agendaItemService.count() > 0) return;

        // ── Programações fixas ────────────────────────────────────
        agendaItemService.save(AgendaItem.builder()
                .type("fixed").name("EBD — Escola Bíblica Dominical")
                .dayOfWeek(0).time("08:45").frequency("Todo domingo")
                .location("Salão Principal")
                .description("Palavra, reflexão e aplicação para a vida real.")
                .icon("book-open").color("cyan").highlight(false).build());

        agendaItemService.save(AgendaItem.builder()
                .type("fixed").name("Culto de Jovens")
                .dayOfWeek(0).time("10:00").frequency("Todo domingo")
                .location("Salão Principal")
                .description("Louvor, Palavra e comunhão. O culto que nos une como geração.")
                .icon("music-note-beamed").color("green").highlight(true).build());

        agendaItemService.save(AgendaItem.builder()
                .type("fixed").name("Célula Semanal")
                .dayOfWeek(4).time("19:30").frequency("Toda semana")
                .location("Casas dos membros")
                .description("GCG — Gente Cuidando de Gente. Encontros em pequenos grupos.")
                .icon("people-fill").color("purple").highlight(false).build());

        // ── Eventos datados ───────────────────────────────────────
        int y = LocalDate.now().getYear();

        agendaItemService.save(AgendaItem.builder()
                .type("event").name("Encontro de Casais")
                .date(LocalDate.of(y, 4, 19)).time("19:00")
                .location("Salão Principal")
                .description("Um encontro especial para fortalecer os relacionamentos.")
                .icon("heart-fill").color("red").highlight(false).build());

        agendaItemService.save(AgendaItem.builder()
                .type("event").name("Noite de Louvor")
                .date(LocalDate.of(y, 4, 26)).time("19:30")
                .location("Salão Principal")
                .description("Uma noite inteiramente dedicada ao louvor e adoração.")
                .icon("music-note-list").color("purple").highlight(false).build());

        agendaItemService.save(AgendaItem.builder()
                .type("event").name("Acampamento de Jovens")
                .date(LocalDate.of(y, 5, 16)).time("08:00")
                .location("Sítio dos Sonhos")
                .description("Três dias de imersão, louvor e Palavra. Venha transformado.")
                .icon("tree-fill").color("green").highlight(true).build());

        agendaItemService.save(AgendaItem.builder()
                .type("event").name("Estudo Temático — Ansiedade")
                .date(LocalDate.of(y, 6, 7)).time("19:00")
                .location("Online (YouTube)")
                .description("Como a Bíblia responde ao maior desafio emocional da nossa geração.")
                .icon("lightbulb-fill").color("orange").highlight(false).build());

        log.info("Agenda padrão criada com {} itens", agendaItemService.count());
    }
}

