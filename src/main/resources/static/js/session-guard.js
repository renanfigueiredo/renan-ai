/* =====================================================
   EVJ AI — Session guard
   Detecta 401 em qualquer fetch (incluindo SSE) e redireciona
   o usuário para /login?next=<path-atual>&session=expired,
   mostrando um toast curto antes do redirect.
   ===================================================== */

(function () {
    'use strict';

    if (window.__evjSessionGuardInstalled) return;
    window.__evjSessionGuardInstalled = true;

    // Exposto globalmente para que callers (sendMessage, etc.) possam evitar
    // mostrar toast de erro confuso quando já estamos redirecionando.
    window.__evjAuthRedirecting = false;
    let redirectingDueToAuth = false;

    function currentPathWithQuery() {
        return window.location.pathname + window.location.search + window.location.hash;
    }

    function buildLoginUrl() {
        const next = encodeURIComponent(currentPathWithQuery());
        return '/login?session=expired&next=' + next;
    }

    function redirectToLogin() {
        if (redirectingDueToAuth) return;
        redirectingDueToAuth = true;
        window.__evjAuthRedirecting = true;

        // Toast best-effort — funciona se app.js já carregou
        try {
            if (typeof showToast === 'function') {
                showToast('Sessão expirada. Redirecionando para o login…', 'warning', 1800);
            }
        } catch (_) {}

        // Pequeno delay para o usuário ver o toast
        setTimeout(() => { window.location.href = buildLoginUrl(); }, 450);
    }

    /**
     * Considera "auth expirou" se:
     *   - status === 401, OU
     *   - status === 403 e o header X-Auth-Required estiver presente.
     * O 403 normal de "sem permissão" (ex: /api/admin/...) NÃO redireciona,
     * porque o usuário continua logado — apenas não tem acesso.
     */
    function isAuthExpired(response) {
        if (!response) return false;
        if (response.status === 401) return true;
        if (response.status === 403 && response.headers && response.headers.get('X-Auth-Required') === '1') {
            return true;
        }
        return false;
    }

    // ── Patch global em window.fetch ────────────────────────────
    const origFetch = window.fetch.bind(window);
    window.fetch = async function (input, init) {
        let response;
        try {
            response = await origFetch(input, init);
        } catch (e) {
            // Erro de rede — propaga sem mexer
            throw e;
        }
        if (isAuthExpired(response)) {
            redirectToLogin();
        }
        return response;
    };

    // ── EventSource (caso seja usado em algum lugar) ────────────
    // Hoje o chat usa fetch+ReadableStream para SSE, não EventSource,
    // mas se alguém usar EventSource o onerror também vai disparar.
    if (typeof window.EventSource === 'function') {
        const OrigES = window.EventSource;
        function PatchedES(url, init) {
            const es = new OrigES(url, init);
            es.addEventListener('error', () => {
                // EventSource não expõe status code; fazemos um probe leve
                fetch('/api/me', { headers: { 'Accept': 'application/json' } }).catch(() => {});
            });
            return es;
        }
        PatchedES.prototype = OrigES.prototype;
        window.EventSource = PatchedES;
    }
})();
