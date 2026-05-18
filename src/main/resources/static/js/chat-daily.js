/* =====================================================
   EVJ AI — Daily card (versículo do dia + sugestões)
   Renderiza dentro do welcome message quando a conversa
   está vazia. É um card informativo, não substitui a saudação.
   ===================================================== */

(function () {
    'use strict';

    if (!document.getElementById('messageInput')) return;

    const SUGGESTION_ICONS = [
        'bi-megaphone',
        'bi-sun',
        'bi-bookmark-star',
        'bi-mortarboard',
        'bi-search',
        'bi-question-circle',
        'bi-heart',
        'bi-stars'
    ];

    document.addEventListener('DOMContentLoaded', () => {
        const welcome = document.getElementById('welcomeMessage');
        if (!welcome) return; // Já tem mensagens — não mostra
        loadDaily(welcome);
    });

    async function loadDaily(welcome) {
        try {
            const res = await fetch('/api/chat/daily');
            if (!res.ok) return;
            const data = await res.json();
            renderCard(welcome, data);
        } catch (_) { /* silencioso */ }
    }

    function renderCard(welcome, data) {
        const card = document.createElement('div');
        card.className = 'evj-daily-card';

        const dateStr = formatDate(data.date);
        const verseHtml = data.verse ? `
            <div class="evj-daily-verse">${escapeHtml(data.verse.text)}</div>
            <div class="evj-daily-verse-ref">— ${escapeHtml(data.verse.reference)}</div>
        ` : '';

        const suggestionsHtml = (data.suggestions || []).map((s, i) => `
            <button class="evj-daily-chip" data-prompt="${escapeAttr(s)}">
                <i class="bi ${SUGGESTION_ICONS[i % SUGGESTION_ICONS.length]}"></i>
                <span>${escapeHtml(s)}</span>
            </button>
        `).join('');

        card.innerHTML = `
            <div class="evj-daily-header">
                <span class="evj-daily-kicker"><i class="bi bi-sunrise"></i> Versículo do dia</span>
                <span class="evj-daily-date">${dateStr}</span>
            </div>
            ${verseHtml}
            <div class="evj-daily-suggestions-label">Comece com uma destas ideias</div>
            <div class="evj-daily-suggestions">${suggestionsHtml}</div>
        `;

        card.addEventListener('click', (e) => {
            const chip = e.target.closest('.evj-daily-chip');
            if (!chip) return;
            const prompt = chip.dataset.prompt;
            const ta = document.getElementById('messageInput');
            if (!ta) return;
            ta.value = prompt;
            ta.dispatchEvent(new Event('input'));
            ta.focus();
            // Scroll para input + foco
            ta.scrollIntoView({ behavior: 'smooth', block: 'end' });
        });

        welcome.appendChild(card);
    }

    function formatDate(iso) {
        if (!iso) return '';
        try {
            const d = new Date(iso + 'T00:00:00');
            const days = ['Domingo','Segunda','Terça','Quarta','Quinta','Sexta','Sábado'];
            const months = ['jan','fev','mar','abr','mai','jun','jul','ago','set','out','nov','dez'];
            return `${days[d.getDay()]}, ${d.getDate()} de ${months[d.getMonth()]}`;
        } catch (_) { return iso; }
    }

    function escapeHtml(t) {
        const d = document.createElement('div');
        d.textContent = t || '';
        return d.innerHTML;
    }
    function escapeAttr(t) {
        return (t || '').replace(/"/g, '&quot;').replace(/&/g, '&amp;');
    }
})();
