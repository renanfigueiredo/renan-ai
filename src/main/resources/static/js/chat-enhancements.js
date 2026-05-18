/* =====================================================
   EVJ AI — Chat Enhancements
   Acoplamento mínimo: lê a App global de app.js, mas roda
   isolado em listeners próprios. Pode ser desativado removendo
   o <script src="...">.
   ===================================================== */

(function () {
    'use strict';

    // Roda apenas na página /chat (presença do textarea principal)
    if (!document.getElementById('messageInput')) return;

    // ── Slash commands ──────────────────────────────────────────
    const SLASH_COMMANDS = [
        { cmd: '/sermao',      icon: 'bi-megaphone',     label: 'Esboço de sermão',
          template: 'Prepare um esboço expositivo de sermão sobre {tópico ou texto bíblico}, com introdução, 3 pontos principais e aplicação.' },
        { cmd: '/devocional',  icon: 'bi-sun',           label: 'Devocional curto',
          template: 'Faça um devocional curto sobre {tema ou versículo}, com leitura, reflexão e oração.' },
        { cmd: '/versiculo',   icon: 'bi-bookmark-star', label: 'Buscar versículo',
          template: 'Quais versículos da Bíblia falam sobre {tema}? Liste os mais importantes com referência completa.' },
        { cmd: '/ebd',         icon: 'bi-mortarboard',   label: 'Lição de EBD',
          template: 'Prepare uma lição de EBD para jovens sobre {tema}, com perguntas para discussão em classe.' },
        { cmd: '/exegese',     icon: 'bi-search',        label: 'Exegese de texto',
          template: 'Faça uma exegese curta de {referência bíblica}: contexto histórico, estrutura literária, sentido original e aplicação.' },
        { cmd: '/duvida',      icon: 'bi-question-circle', label: 'Dúvida doutrinária',
          template: 'Tenho uma dúvida sobre {doutrina}. Explique do ponto de vista reformado, com base na Escritura.' },
        { cmd: '/aconselhar',  icon: 'bi-heart',         label: 'Aconselhamento pastoral',
          template: 'Estou passando por {situação}. Como a Bíblia me orienta? Seja prático e pastoral.' },
        { cmd: '/melhorar',    icon: 'bi-magic',         label: 'Melhorar este prompt',
          action: 'enhance' }
    ];

    let slashOpen = false;
    let slashHighlight = 0;

    function ensureSlashMenu() {
        let m = document.getElementById('slashMenu');
        if (m) return m;
        m = document.createElement('div');
        m.id = 'slashMenu';
        m.className = 'slash-menu';
        m.setAttribute('role', 'listbox');
        m.style.display = 'none';
        document.body.appendChild(m);
        return m;
    }

    function renderSlashMenu(filter) {
        const menu = ensureSlashMenu();
        const matches = SLASH_COMMANDS.filter(c =>
            c.cmd.startsWith(filter) || c.label.toLowerCase().includes(filter.slice(1).toLowerCase()));
        if (matches.length === 0) { hideSlashMenu(); return; }
        slashHighlight = Math.min(slashHighlight, matches.length - 1);
        menu.innerHTML = matches.map((c, i) => `
            <div class="slash-item ${i === slashHighlight ? 'active' : ''}"
                 data-cmd="${c.cmd}" role="option">
                <i class="bi ${c.icon}"></i>
                <div class="slash-text">
                    <span class="slash-cmd">${c.cmd}</span>
                    <span class="slash-label">${c.label}</span>
                </div>
            </div>
        `).join('');
        menu.querySelectorAll('.slash-item').forEach(el => {
            el.addEventListener('mousedown', (e) => {
                e.preventDefault();
                applySlash(el.dataset.cmd);
            });
        });

        // Posiciona acima do textarea
        const ta = document.getElementById('messageInput');
        if (!ta) return;
        const rect = ta.getBoundingClientRect();
        menu.style.left = rect.left + 'px';
        menu.style.bottom = (window.innerHeight - rect.top + 6) + 'px';
        menu.style.width = Math.min(rect.width, 360) + 'px';
        menu.style.display = 'block';
        slashOpen = true;
        menu.dataset.matches = JSON.stringify(matches.map(c => c.cmd));
    }

    function hideSlashMenu() {
        const menu = document.getElementById('slashMenu');
        if (menu) menu.style.display = 'none';
        slashOpen = false;
        slashHighlight = 0;
    }

    function applySlash(cmd) {
        const cmdObj = SLASH_COMMANDS.find(c => c.cmd === cmd);
        const ta = document.getElementById('messageInput');
        if (!cmdObj || !ta) return;

        if (cmdObj.action === 'enhance') {
            hideSlashMenu();
            if (typeof enhanceChatPrompt === 'function') enhanceChatPrompt();
            return;
        }

        // Substitui a parte slash (até primeiro espaço) pelo template
        const v = ta.value;
        const match = v.match(/(^|\n)(\/\S*)$/);
        if (match) {
            ta.value = v.slice(0, v.length - match[2].length) + cmdObj.template;
        } else {
            ta.value = cmdObj.template;
        }
        ta.dispatchEvent(new Event('input'));
        ta.focus();

        // Posiciona cursor no primeiro placeholder {...}
        const placeholderMatch = ta.value.match(/\{[^}]+\}/);
        if (placeholderMatch) {
            const start = placeholderMatch.index;
            ta.setSelectionRange(start, start + placeholderMatch[0].length);
        }
        hideSlashMenu();
    }

    function initSlashCommands() {
        const ta = document.getElementById('messageInput');
        if (!ta) return;

        ta.addEventListener('input', () => {
            const v = ta.value;
            const caret = ta.selectionStart;
            const before = v.slice(0, caret);
            // Detecta /token apenas no início ou após nova linha
            const m = before.match(/(^|\n)(\/\S*)$/);
            if (m) {
                renderSlashMenu(m[2]);
            } else {
                hideSlashMenu();
            }
        });

        ta.addEventListener('keydown', (e) => {
            if (!slashOpen) return;
            const menu = document.getElementById('slashMenu');
            if (!menu) return;
            const items = JSON.parse(menu.dataset.matches || '[]');
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                slashHighlight = (slashHighlight + 1) % items.length;
                const v = ta.value;
                const m = v.slice(0, ta.selectionStart).match(/(^|\n)(\/\S*)$/);
                renderSlashMenu(m ? m[2] : '/');
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                slashHighlight = (slashHighlight - 1 + items.length) % items.length;
                const v = ta.value;
                const m = v.slice(0, ta.selectionStart).match(/(^|\n)(\/\S*)$/);
                renderSlashMenu(m ? m[2] : '/');
            } else if (e.key === 'Enter' || e.key === 'Tab') {
                e.preventDefault();
                if (items[slashHighlight]) applySlash(items[slashHighlight]);
            } else if (e.key === 'Escape') {
                hideSlashMenu();
            }
        });

        document.addEventListener('click', (e) => {
            if (!e.target.closest('#slashMenu') && e.target !== ta) hideSlashMenu();
        });
    }

    // ── Stop generation ──────────────────────────────────────────
    let activeAbortController = null;

    function installStopButton() {
        const sendBtn = document.getElementById('sendBtn');
        if (!sendBtn) return;
        if (document.getElementById('stopBtn')) return;
        const stop = document.createElement('button');
        stop.id = 'stopBtn';
        stop.className = 'send-btn stop-btn';
        stop.title = 'Parar geração';
        stop.style.display = 'none';
        stop.innerHTML = '<i class="bi bi-stop-fill"></i>';
        stop.addEventListener('click', () => {
            if (activeAbortController) {
                activeAbortController.abort();
                activeAbortController = null;
            }
        });
        sendBtn.parentNode.insertBefore(stop, sendBtn.nextSibling);
    }

    /**
     * Patch leve em window.fetch: quando o chat dispara /api/chat/stream,
     * registramos o AbortController para que o botão Parar funcione, e
     * alternamos a visibilidade enviar↔parar.
     */
    function patchFetchForStop() {
        const origFetch = window.fetch.bind(window);
        window.fetch = function (input, init) {
            const url = typeof input === 'string' ? input : (input && input.url) || '';
            if (url.includes('/api/chat/stream')) {
                const ctrl = new AbortController();
                activeAbortController = ctrl;
                init = init || {};
                init.signal = ctrl.signal;

                const sendBtn = document.getElementById('sendBtn');
                const stopBtn = document.getElementById('stopBtn');
                if (sendBtn) sendBtn.style.display = 'none';
                if (stopBtn) stopBtn.style.display = '';

                return origFetch(input, init).finally(() => {
                    activeAbortController = null;
                    if (sendBtn) sendBtn.style.display = '';
                    if (stopBtn) stopBtn.style.display = 'none';
                });
            }
            return origFetch(input, init);
        };
    }

    // ── Hover actions na resposta ───────────────────────────────
    function installHoverActions() {
        // Adiciona ações em mensagens já renderizadas
        document.querySelectorAll('.message-wrapper.assistant-side .message-body, .message.assistant')
            .forEach(addActionsToMessage);

        // Observa mensagens novas
        const area = document.getElementById('messagesArea');
        if (!area) return;
        const obs = new MutationObserver((mutations) => {
            mutations.forEach(m => {
                m.addedNodes.forEach(node => {
                    if (node.nodeType !== 1) return;
                    if (node.matches?.('.message-wrapper.assistant-side, .message.assistant')) {
                        addActionsToMessage(node);
                    }
                    node.querySelectorAll?.('.message-wrapper.assistant-side .message-body, .message.assistant')
                        .forEach(addActionsToMessage);
                });
            });
        });
        obs.observe(area, { childList: true, subtree: true });
    }

    function addActionsToMessage(el) {
        // Encontra o body — pode ser .message-body ou .message-content do streaming
        const body = el.querySelector?.('.message-body') || el.querySelector?.('.message-content') || el;
        if (!body) return;
        if (body.querySelector('.evj-actions')) return; // já tem

        const actions = document.createElement('div');
        actions.className = 'evj-actions';
        actions.innerHTML = `
            <button class="evj-act-btn" data-act="copy" title="Copiar"><i class="bi bi-clipboard"></i></button>
            <button class="evj-act-btn" data-act="speak" title="Ler em voz alta"><i class="bi bi-volume-up"></i></button>
            <button class="evj-act-btn" data-act="regen" title="Regenerar"><i class="bi bi-arrow-repeat"></i></button>
            <button class="evj-act-btn" data-act="export" title="Exportar como Markdown"><i class="bi bi-file-earmark-arrow-down"></i></button>
        `;
        actions.addEventListener('click', (ev) => {
            const btn = ev.target.closest('.evj-act-btn');
            if (!btn) return;
            const proseEl = body.querySelector('.prose, .message-bubble');
            const text = proseEl ? proseEl.innerText : body.innerText;
            const html = proseEl ? proseEl.innerHTML : body.innerHTML;
            const act = btn.dataset.act;
            if (act === 'copy') {
                navigator.clipboard.writeText(text).then(() => toast('Copiado'));
            } else if (act === 'speak') {
                speakText(text, btn);
            } else if (act === 'regen') {
                regenerateLastResponse();
            } else if (act === 'export') {
                exportMessage(text);
            }
        });
        body.appendChild(actions);
    }

    function speakText(text, btn) {
        if (!('speechSynthesis' in window)) {
            toast('Seu navegador não suporta síntese de voz', 'error');
            return;
        }
        const synth = window.speechSynthesis;
        if (synth.speaking) {
            synth.cancel();
            btn.classList.remove('speaking');
            return;
        }
        const utter = new SpeechSynthesisUtterance(text.slice(0, 4000));
        utter.lang = 'pt-BR';
        utter.rate = 1.0;
        const voices = synth.getVoices();
        const ptVoice = voices.find(v => v.lang.startsWith('pt'));
        if (ptVoice) utter.voice = ptVoice;
        utter.onend = () => btn.classList.remove('speaking');
        utter.onerror = () => btn.classList.remove('speaking');
        btn.classList.add('speaking');
        synth.speak(utter);
    }

    function regenerateLastResponse() {
        // Pega a última mensagem do usuário no DOM e dispara sendMessage com ela
        const userMsgs = document.querySelectorAll('.message-wrapper.user-side .message-content, .user-message .message-content');
        if (userMsgs.length === 0) { toast('Nenhuma pergunta para regenerar'); return; }
        const lastUserText = userMsgs[userMsgs.length - 1].innerText.trim();
        const ta = document.getElementById('messageInput');
        if (!ta) return;
        ta.value = lastUserText;
        ta.dispatchEvent(new Event('input'));
        if (typeof sendMessage === 'function') sendMessage();
    }

    function exportMessage(text) {
        const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        const date = new Date().toISOString().slice(0, 10);
        a.href = url;
        a.download = `evj-resposta-${date}.md`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    function toast(msg, type = 'info') {
        if (typeof showToast === 'function') showToast(msg, type, 2000);
        else console.log('[EVJ]', msg);
    }

    // ── Follow-up suggestions após resposta ─────────────────────
    function installFollowupTrigger() {
        // Espera o evento "done" no fim do streaming. Usamos MutationObserver
        // para detectar quando uma mensagem assistente perde a classe `streaming`.
        const area = document.getElementById('messagesArea');
        if (!area) return;
        const obs = new MutationObserver(() => {
            const last = area.querySelector('.message.assistant:not(.streaming):last-of-type, .message-wrapper.assistant-side:last-of-type');
            if (!last) return;
            if (last.dataset.followupsDone === '1') return;
            // Só dispara se já houver conteúdo finalizado (sem cursor blink)
            if (last.querySelector('.cursor-blink')) return;
            last.dataset.followupsDone = '1';
            requestFollowups(last);
        });
        obs.observe(area, { childList: true, subtree: true, attributes: true, attributeFilter: ['class'] });
    }

    async function requestFollowups(messageEl) {
        // Encontra a última pergunta do usuário (irmã anterior)
        const userMsgs = document.querySelectorAll('.message-wrapper.user-side .message-content, .user-message .message-content');
        if (userMsgs.length === 0) return;
        const userMessage = userMsgs[userMsgs.length - 1].innerText.trim();
        const proseEl = messageEl.querySelector('.prose, .message-bubble');
        if (!proseEl) return;
        const assistantReply = proseEl.innerText.trim();
        if (assistantReply.length < 50) return; // resposta muito curta — não vale

        try {
            const res = await fetch('/api/chat/followups', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userMessage, assistantReply })
            });
            if (!res.ok) return;
            const data = await res.json();
            const followups = data.followups || [];
            if (followups.length === 0) return;
            renderFollowups(messageEl, followups);
        } catch (_) { /* silencioso — best effort */ }
    }

    function renderFollowups(messageEl, followups) {
        const wrap = document.createElement('div');
        wrap.className = 'evj-followups';
        wrap.innerHTML = `
            <div class="evj-followups-label"><i class="bi bi-lightbulb"></i> Aprofunde o estudo</div>
            <div class="evj-followups-chips">
                ${followups.map(q => `
                    <button class="evj-followup-chip" type="button">${escapeHtml(q)}</button>
                `).join('')}
            </div>
        `;
        wrap.querySelectorAll('.evj-followup-chip').forEach(btn => {
            btn.addEventListener('click', () => {
                const ta = document.getElementById('messageInput');
                if (!ta) return;
                ta.value = btn.textContent.trim();
                ta.dispatchEvent(new Event('input'));
                if (typeof sendMessage === 'function') sendMessage();
            });
        });
        messageEl.appendChild(wrap);
    }

    function escapeHtml(t) {
        const d = document.createElement('div');
        d.textContent = t;
        return d.innerHTML;
    }

    // ── Histórico agrupado por data ─────────────────────────────
    function installHistoryGrouping() {
        const list = document.getElementById('convList');
        if (!list) return;

        const apply = () => groupConvList(list);
        // primeira renderização (server-rendered)
        apply();
        // re-renderizações via JS — observa
        const obs = new MutationObserver(() => {
            // evita loop: só agrupa se ainda não tem headers
            if (!list.querySelector('.conv-group-header')) apply();
        });
        obs.observe(list, { childList: true });
    }

    function groupConvList(list) {
        const items = Array.from(list.querySelectorAll(':scope > .conv-item'));
        if (items.length === 0) return;
        // se ainda há marcadores de "empty" não agrupa
        if (list.querySelector('.empty-convs')) return;

        const now = new Date();
        const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const startOfYesterday = new Date(startOfToday); startOfYesterday.setDate(startOfYesterday.getDate() - 1);
        const startOf7d = new Date(startOfToday); startOf7d.setDate(startOf7d.getDate() - 7);
        const startOf30d = new Date(startOfToday); startOf30d.setDate(startOf30d.getDate() - 30);

        const groups = { 'Fixadas': [], 'Hoje': [], 'Ontem': [], '7 dias': [], '30 dias': [], 'Mais antigas': [] };

        items.forEach(it => {
            const meta = it.querySelector('.conv-item-meta');
            const ts = it.dataset.updated ? new Date(it.dataset.updated)
                : (meta && meta.dataset.ts) ? new Date(meta.dataset.ts) : null;
            if (it.querySelector('.pin-badge')) { groups['Fixadas'].push(it); return; }
            if (!ts || isNaN(ts)) { groups['Mais antigas'].push(it); return; }
            if (ts >= startOfToday) groups['Hoje'].push(it);
            else if (ts >= startOfYesterday) groups['Ontem'].push(it);
            else if (ts >= startOf7d) groups['7 dias'].push(it);
            else if (ts >= startOf30d) groups['30 dias'].push(it);
            else groups['Mais antigas'].push(it);
        });

        // Se nenhum item tem timestamp, não agrupa (evita resultado pior)
        const totalDated = Object.entries(groups).filter(([k]) => k !== 'Mais antigas')
            .reduce((s, [, arr]) => s + arr.length, 0);
        if (totalDated === 0 && groups['Fixadas'].length === 0) return;

        // Re-monta a lista
        const frag = document.createDocumentFragment();
        Object.entries(groups).forEach(([label, arr]) => {
            if (arr.length === 0) return;
            const h = document.createElement('div');
            h.className = 'conv-group-header';
            h.textContent = label;
            frag.appendChild(h);
            arr.forEach(it => frag.appendChild(it));
        });
        list.innerHTML = '';
        list.appendChild(frag);
    }

    // ── Inicialização ────────────────────────────────────────────
    document.addEventListener('DOMContentLoaded', () => {
        try {
            initSlashCommands();
            installStopButton();
            patchFetchForStop();
            installHoverActions();
            installFollowupTrigger();
            installHistoryGrouping();
        } catch (e) {
            console.error('[EVJ enhancements] erro ao inicializar', e);
        }
    });
})();
