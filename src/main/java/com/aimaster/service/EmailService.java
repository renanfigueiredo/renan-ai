package com.aimaster.service;

import com.aimaster.config.AppProperties;
import com.aimaster.config.MailgunProperties;
import com.aimaster.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Base64;

/**
 * E-mail service using Mailgun HTTP API with Spring Boot 4's RestClient
 * and type-safe configuration via Java 25 records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestClient restClient = RestClient.create();
    private final AppProperties appProperties;
    private final MailgunProperties mailgunProperties;

    /** Notifica o administrador de que um novo usuário solicitou acesso. */
    public void sendApprovalRequest(AppUser user) {
        var approveUrl = appProperties.baseUrl() + "/admin/approve/" + user.getApprovalToken();
        var rejectUrl  = appProperties.baseUrl() + "/admin/reject/"  + user.getApprovalToken();

        var html = """
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

        send(appProperties.adminEmail(), "EVJ AI – Solicitação de acesso: " + user.getName(), html);
    }

    /** Notifica o usuário de que sua conta foi aprovada. */
    public void sendAccountApproved(AppUser user) {
        var loginUrl = appProperties.baseUrl() + "/login";
        var html = """
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

    public void sendPasswordReset(AppUser user, String rawToken) {
        var resetUrl = appProperties.baseUrl() + "/reset-password?token=" + rawToken;
        var html = """
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
        var url = "https://api.mailgun.net/v3/" + mailgunProperties.domain() + "/messages";
        var auth = Base64.getEncoder().encodeToString(("api:" + mailgunProperties.apiKey()).getBytes());

        var body = new LinkedMultiValueMap<String, String>();
        body.add("from", appProperties.mailFrom());
        body.add("to", to);
        body.add("subject", subject);
        body.add("html", htmlBody);

        restClient.post()
                .uri(url)
                .header("Authorization", "Basic " + auth)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("E-mail enviado para {}: {}", to, subject);
    }
}
