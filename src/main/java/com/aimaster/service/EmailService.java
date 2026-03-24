package com.aimaster.service;

import com.aimaster.model.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Service
public class EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.admin-email:renan.figueiredo.05@gmail.com}")
    private String adminEmail;

    @Value("${app.mail-from:EVJ AI <postmaster@renan-ai.com.br>}")
    private String mailFrom;

    @Value("${mailgun.api-key}")
    private String mailgunApiKey;

    @Value("${mailgun.domain:renan-ai.com.br}")
    private String mailgunDomain;

    /** Notifica o administrador de que um novo usuário solicitou acesso. */
    public void sendApprovalRequest(AppUser user) {
        String approveUrl = baseUrl + "/admin/approve/" + user.getApprovalToken();
        String rejectUrl  = baseUrl + "/admin/reject/"  + user.getApprovalToken();

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#00ccff">EVJ AI — Nova Solicitação de Acesso</h2>
                  <p>Um novo usuário solicitou acesso à plataforma:</p>
                  <table style="border-collapse:collapse;width:100%%">
                    <tr><td style="padding:8px;font-weight:bold">Nome</td><td style="padding:8px">%s</td></tr>
                    <tr><td style="padding:8px;font-weight:bold">E-mail</td><td style="padding:8px">%s</td></tr>
                  </table>
                  <p style="margin-top:24px">
                    <a href="%s" style="background:#16a34a;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none;margin-right:12px">✅ Aprovar</a>
                    <a href="%s" style="background:#dc2626;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none">❌ Rejeitar</a>
                  </p>
                </div>
                """.formatted(user.getName(), user.getEmail(), approveUrl, rejectUrl);

        send(adminEmail, "EVJ AI – Solicitação de acesso: " + user.getName(), html);
    }

    /** Notifica o usuário de que sua conta foi aprovada. */
    public void sendAccountApproved(AppUser user) {
        String loginUrl = baseUrl + "/login";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#00ccff">EVJ AI — Acesso Aprovado! 🎉</h2>
                  <p>Olá, <strong>%s</strong>!</p>
                  <p>Sua solicitação de acesso foi <strong style="color:#16a34a">aprovada</strong>.</p>
                  <p>Você já pode fazer login e começar a usar a plataforma:</p>
                  <p><a href="%s" style="background:#00ccff;color:#0e0a1a;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:700">Acessar EVJ AI</a></p>
                </div>
                """.formatted(user.getName(), loginUrl);

        send(user.getEmail(), "EVJ AI – Seu acesso foi aprovado!", html);
    }

    /**
     * Envia link de reset de senha.
     * @param user usuário que solicitou o reset
     * @param rawToken token em texto puro (UUID) — não o hash armazenado no banco
     */
    public void sendPasswordReset(AppUser user, String rawToken) {
        String resetUrl = baseUrl + "/reset-password?token=" + rawToken;
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#00ccff">EVJ AI — Redefinição de Senha</h2>
                  <p>Olá, <strong>%s</strong>!</p>
                  <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
                  <p>Clique no botão abaixo para criar uma nova senha. O link é válido por <strong>1 hora</strong> e só pode ser usado uma vez.</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#7c3aed;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none">Redefinir minha senha</a>
                  </p>
                  <p style="color:#6b7280;font-size:.85rem">Se você não solicitou este reset, ignore este e-mail. Sua senha permanece a mesma.</p>
                  <hr style="border:none;border-top:1px solid #374151;margin:24px 0">
                  <p style="color:#6b7280;font-size:.8rem">Link completo: <a href="%s" style="color:#7c3aed">%s</a></p>
                </div>
                """.formatted(user.getName(), resetUrl, resetUrl, resetUrl);

        send(user.getEmail(), "EVJ AI – Redefinição de senha", html);
    }

    private void send(String to, String subject, String htmlBody) {
        String url = "https://api.mailgun.net/v3/" + mailgunDomain + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        String auth = Base64.getEncoder().encodeToString(("api:" + mailgunApiKey).getBytes());
        headers.set("Authorization", "Basic " + auth);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("from", mailFrom);
        body.add("to", to);
        body.add("subject", subject);
        body.add("html", htmlBody);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
        log.info("E-mail enviado para {}: {}", to, subject);
    }
}
