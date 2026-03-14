package com.aimaster.controller;

import com.aimaster.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("error", "E-mail ou senha inválidos. Verifique se sua conta foi aprovada.");
        if (logout != null) model.addAttribute("message", "Você saiu com sucesso.");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam @NotBlank String name,
            @RequestParam @Email @NotBlank String email,
            @RequestParam @Size(min = 8) String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttrs) {

        if (!password.equals(confirmPassword)) {
            redirectAttrs.addFlashAttribute("error", "As senhas não coincidem.");
            return "redirect:/register";
        }

        try {
            userService.register(name, email, password);
            return "redirect:/pending";
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            log.error("Erro no registro", e);
            redirectAttrs.addFlashAttribute("error", "Erro ao processar cadastro. Tente novamente.");
            return "redirect:/register";
        }
    }

    @GetMapping("/pending")
    public String pendingPage() {
        return "pending";
    }

    @GetMapping("/admin/approve/{token}")
    public String approveUser(@PathVariable String token, Model model) {
        boolean ok = userService.approveUser(token);
        model.addAttribute("success", ok);
        model.addAttribute("action", "aprovado");
        return "admin-action";
    }

    @GetMapping("/admin/reject/{token}")
    public String rejectUser(@PathVariable String token, Model model) {
        boolean ok = userService.rejectUser(token);
        model.addAttribute("success", ok);
        model.addAttribute("action", "rejeitado");
        return "admin-action";
    }

    @GetMapping("/api/me")
    @ResponseBody
    public ResponseEntity<Map<String, String>> me(Principal principal) {
        return userService.findByEmail(principal.getName())
                .map(u -> {
                    String displayEmail = u.getEmail().endsWith("@guest.invalid")
                            ? "Modo Visitante"
                            : u.getEmail();
                    return ResponseEntity.ok(Map.of("name", u.getName(), "email", displayEmail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Esqueci minha senha ────────────────────────────────────────

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam @Email @NotBlank String email,
                                 RedirectAttributes redirectAttrs) {
        // Sempre responde com a mesma mensagem — não revela se o e-mail existe
        try {
            userService.requestPasswordReset(email);
        } catch (Exception e) {
            log.error("Erro ao processar reset para {}", email, e);
        }
        redirectAttrs.addFlashAttribute("message",
                "Se esse e-mail estiver cadastrado, você receberá as instruções em breve.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        return userService.validateResetToken(token)
                .map(email -> {
                    model.addAttribute("token", token);
                    model.addAttribute("email", email);
                    return "reset-password";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Link inválido ou expirado. Solicite um novo reset.");
                    return "reset-password";
                });
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam @Size(min = 8) String password,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirectAttrs,
                                Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "As senhas não coincidem.");
            // Re-validate token to re-populate email
            userService.validateResetToken(token).ifPresent(e -> model.addAttribute("email", e));
            return "reset-password";
        }

        try {
            userService.resetPassword(token, password);
            redirectAttrs.addFlashAttribute("message", "Senha redefinida com sucesso! Faça login com sua nova senha.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "reset-password";
        }
    }
}
