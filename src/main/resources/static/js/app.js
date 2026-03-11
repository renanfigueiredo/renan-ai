/* =====================================================
   AI MASTER STUDIO — Main JavaScript
   ===================================================== */

'use strict';

// ── Global State ──────────────────────────────────────
const App = {
    theme: localStorage.getItem('theme') || 'dark',
    sidebarCollapsed: localStorage.getItem('sidebarCollapsed') === 'true',
    currentConversationId: null,
    currentModel: null,
    isGenerating: false,
    attachedImage: null,
    attachedDocument: null,
    videoPollers: {},   // { videoId: intervalId }
    galleryItems: [],
    lightboxIdx: 0,
};

// ── Init ──────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initSidebar();
    initPageSpecific();
    initHighlightJs();
    checkPendingTemplate();
});

function initHighlightJs() {
    if (window.hljs) {
        document.querySelectorAll('pre code').forEach(el => hljs.highlightElement(el));
    }
}

// ── Theme ─────────────────────────────────────────────
function initTheme() {
    if (App.theme === 'light') {
        document.body.classList.add('light-mode');
    }
    updateThemeIcon();
}

function toggleTheme() {
    App.theme = App.theme === 'dark' ? 'light' : 'dark';
    document.body.classList.toggle('light-mode', App.theme === 'light');
    localStorage.setItem('theme', App.theme);
    updateThemeIcon();
}

function updateThemeIcon() {
    const btn = document.querySelector('.theme-toggle');
    const topbarBtn = document.getElementById('topbarThemeBtn');
    const isDark = App.theme === 'dark';

    // Sidebar button
    if (btn) {
        const icon = btn.querySelector('i');
        const span = btn.querySelector('span');
        if (icon) icon.className = isDark ? 'bi bi-brightness-high' : 'bi bi-moon-stars';
        if (span) span.textContent = isDark ? 'Modo Claro' : 'Modo Escuro';
    }
    // Topbar button (icon only)
    if (topbarBtn) {
        const icon = topbarBtn.querySelector('i');
        if (icon) icon.className = isDark ? 'bi bi-brightness-high' : 'bi bi-moon-stars-fill';
        topbarBtn.title = isDark ? 'Modo Claro' : 'Modo Escuro';
    }
}

// ── Sidebar ───────────────────────────────────────────
function initSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main-content');
    if (!sidebar) return;

    if (App.sidebarCollapsed) {
        sidebar.classList.add('collapsed');
        mainContent?.classList.add('sidebar-collapsed');
    }

    // Desktop toggle button (collapses sidebar)
    document.getElementById('sidebarToggle')?.addEventListener('click', toggleSidebar);

    // Mobile hamburger button (slides sidebar in)
    document.getElementById('mobileMenuBtn')?.addEventListener('click', openMobileSidebar);
}

function toggleSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main-content');
    if (!sidebar) return;
    App.sidebarCollapsed = !App.sidebarCollapsed;
    sidebar.classList.toggle('collapsed', App.sidebarCollapsed);
    mainContent?.classList.toggle('sidebar-collapsed', App.sidebarCollapsed);
    localStorage.setItem('sidebarCollapsed', App.sidebarCollapsed);
}

// Mobile sidebar
function openMobileSidebar() {
    document.querySelector('.sidebar')?.classList.add('mobile-open');
}

document.addEventListener('click', e => {
    const sidebar = document.querySelector('.sidebar');
    if (sidebar?.classList.contains('mobile-open') && !sidebar.contains(e.target) && !e.target.closest('.mobile-menu-btn')) {
        sidebar.classList.remove('mobile-open');
    }
});

// ── Toast ─────────────────────────────────────────────
function showToast(message, type = 'info', duration = 3500) {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const icons = { success: 'bi-check-circle-fill', error: 'bi-x-circle-fill', warning: 'bi-exclamation-triangle-fill', info: 'bi-info-circle-fill' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<i class="bi ${icons[type] || icons.info} toast-icon"></i><span>${escHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'toastOut 0.3s ease forwards';
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

// ── Loading Overlay ───────────────────────────────────
function showLoading(text = 'Processando…') {
    const overlay = document.getElementById('loadingOverlay');
    const txt = document.getElementById('loadingText');
    if (overlay) { overlay.style.display = 'flex'; }
    if (txt) txt.textContent = text;
}

function hideLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) overlay.style.display = 'none';
}

// ── Utilities ─────────────────────────────────────────
function escHtml(str) {
    const d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}

function formatDate(iso) {
    const d = new Date(iso);
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' }) + ' ' +
           d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

function toBase64(file) {
    return new Promise((res, rej) => {
        const reader = new FileReader();
        reader.onload = () => res(reader.result.split(',')[1]);
        reader.onerror = rej;
        reader.readAsDataURL(file);
    });
}

async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        showToast('Copiado para a área de transferência!', 'success', 1500);
    } catch {
        showToast('Falha ao copiar', 'error');
    }
}

// ── Page Router ───────────────────────────────────────
function initPageSpecific() {
    const path = window.location.pathname;
    if (path === '/' || path === '/index') { initDashboard(); }
    else if (path.startsWith('/chat')) { initChat(); }
    else if (path.startsWith('/image')) { initImage(); }
    else if (path.startsWith('/video')) { initVideo(); }
    else if (path.startsWith('/history')) { initHistory(); }
    else if (path.startsWith('/templates')) { initTemplatesPage(); }
}

// ─────────────────────────────────────────────────────
// DASHBOARD
// ─────────────────────────────────────────────────────
function initDashboard() {
    // Animate stat numbers
    document.querySelectorAll('.stat-number[data-value]').forEach(el => {
        const target = parseInt(el.dataset.value || el.textContent, 10);
        if (isNaN(target) || target === 0) return;
        let current = 0;
        const step = Math.ceil(target / 40);
        const interval = setInterval(() => {
            current = Math.min(current + step, target);
            el.textContent = current;
            if (current >= target) clearInterval(interval);
        }, 30);
    });
}

// ─────────────────────────────────────────────────────
// CHAT
// ─────────────────────────────────────────────────────
function initChat() {
    const urlParams = new URLSearchParams(window.location.search);
    const convId = urlParams.get('id');
    const modelId = urlParams.get('model');

    App.currentConversationId = convId || null;
    App.currentModel = modelId || document.querySelector('[data-current-model]')?.dataset.currentModel;

    // Load conversations list
    loadConversationsList();

    // Textarea auto-resize
    const ta = document.getElementById('messageInput');
    if (ta) {
        ta.addEventListener('input', () => {
            ta.style.height = 'auto';
            ta.style.height = Math.min(ta.scrollHeight, 200) + 'px';
            const count = document.getElementById('charCount');
            if (count) count.textContent = ta.value.length;
        });

        ta.addEventListener('keydown', e => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
    }

    // Scroll messages to bottom
    scrollToBottom();

    // Load existing conversation if ID present
    if (convId) {
        loadConversation(convId);
    }

    // New chat button
    document.getElementById('btnNewChat')?.addEventListener('click', createNewChat);

    // Conv search
    const srch = document.getElementById('convSearch');
    if (srch) {
        let debounce;
        srch.addEventListener('input', () => {
            clearTimeout(debounce);
            debounce = setTimeout(() => loadConversationsList(srch.value), 300);
        });
    }

    // Range sliders label sync
    document.querySelectorAll('input[type=range][data-label]').forEach(input => {
        const lbl = document.getElementById(input.dataset.label);
        if (lbl) {
            lbl.textContent = input.value;
            input.addEventListener('input', () => lbl.textContent = input.value);
        }
    });
}

async function loadConversationsList(search = '') {
    try {
        const res = await fetch(`/api/chat/conversations?search=${encodeURIComponent(search)}`);
        const convs = await res.json();
        renderConvList(convs);
    } catch (e) { console.error(e); }
}

function renderConvList(convs) {
    const list = document.getElementById('convList');
    if (!list) return;

    if (!convs.length) {
        list.innerHTML = `<div class="empty-convs"><i class="bi bi-chat-left-dots"></i><span>Nenhuma conversa ainda</span></div>`;
        return;
    }

    list.innerHTML = convs.map(c => `
        <div class="conv-item ${c.id === App.currentConversationId ? 'active' : ''}" onclick="selectConversation('${c.id}')">
            <div class="conv-item-icon"><i class="bi bi-chat-dots"></i>${c.pinned ? '<span class="pin-badge"><i class="bi bi-pin-fill"></i></span>' : ''}</div>
            <div class="conv-item-info">
                <span class="conv-item-title">${escHtml(c.title)}</span>
                <span class="conv-item-meta">${c.messages?.length || 0} msgs · ${escHtml(c.modelId || '')}</span>
            </div>
            <div class="conv-item-actions">
                <button class="conv-action-btn" onclick="event.stopPropagation(); deleteConversation('${c.id}')" title="Excluir"><i class="bi bi-trash"></i></button>
            </div>
        </div>
    `).join('');
}

async function loadConversation(id) {
    try {
        const res = await fetch(`/api/chat/conversation/${id}`);
        if (!res.ok) return;
        const conv = await res.json();
        App.currentConversationId = id;
        App.currentModel = conv.modelId;
        renderMessages(conv.messages);
        updateChatHeader(conv);
        scrollToBottom();
        loadConversationsList();
    } catch (e) { console.error(e); }
}

function selectConversation(id) {
    window.location.href = `/chat?id=${id}`;
}

function renderMessages(messages) {
    const area = document.getElementById('messagesArea');
    if (!area) return;
    const welcome = document.getElementById('welcomeMessage');
    if (welcome) welcome.remove();

    messages.forEach(msg => {
        if (!document.querySelector(`[data-msg-id="${msg.id}"]`)) {
            appendMessage(msg);
        }
    });
}

function appendMessage(msg) {
    const area = document.getElementById('messagesArea');
    if (!area) return;

    const welcome = document.getElementById('welcomeMessage');
    if (welcome) welcome.style.display = 'none';

    const wrapper = document.createElement('div');
    wrapper.setAttribute('data-msg-id', msg.id || '');
    wrapper.className = `message-wrapper ${msg.role === 'user' ? 'user-side' : 'assistant-side'}`;

    if (msg.role === 'user') {
        wrapper.innerHTML = `
            <div class="message user-message">
                <div class="message-content">${escHtml(msg.content)}</div>
                <div class="message-meta">${formatDate(msg.timestamp)}</div>
            </div>`;
    } else {
        wrapper.innerHTML = `
            <div class="assistant-message">
                <div class="message-avatar">🤖</div>
                <div class="message-body">
                    <div class="prose">${msg.formattedContent || escHtml(msg.content)}</div>
                    <div class="message-meta">
                        <span class="meta-model"><i class="bi bi-cpu"></i> ${escHtml(msg.modelId || '')}</span>
                        ${msg.inputTokens ? `<span class="meta-tokens"><i class="bi bi-lightning"></i> ${msg.inputTokens}+${msg.outputTokens}</span>` : ''}
                        ${msg.latencyMs ? `<span class="meta-latency"><i class="bi bi-clock"></i> ${(msg.latencyMs/1000).toFixed(1)}s</span>` : ''}
                        <button class="meta-btn" onclick="copyToClipboard(this.closest('.message-body').querySelector('.prose').innerText)" title="Copiar"><i class="bi bi-clipboard"></i></button>
                    </div>
                </div>
            </div>`;
    }

    area.appendChild(wrapper);
    if (window.hljs) area.querySelectorAll('pre code').forEach(el => hljs.highlightElement(el));
}

async function sendMessage() {
    const ta = document.getElementById('messageInput');
    if (!ta) return;
    const text = ta.value.trim();
    if (!text || App.isGenerating) return;

    App.isGenerating = true;
    const sendBtn = document.getElementById('sendBtn');
    if (sendBtn) sendBtn.disabled = true;

    // Optimistically add user message
    appendMessage({ role: 'user', content: text, timestamp: new Date().toISOString() });
    ta.value = '';
    ta.style.height = 'auto';
    const charCount = document.getElementById('charCount');
    if (charCount) charCount.textContent = '0';

    // Show typing indicator
    showTypingIndicator();
    scrollToBottom();

    // Build request
    const req = {
        conversationId: App.currentConversationId,
        message: text,
        modelId: App.currentModel,
        systemPrompt: document.getElementById('systemPrompt')?.value || '',
        temperature: parseFloat(document.getElementById('temperature')?.value || 0.7),
        maxTokens: parseInt(document.getElementById('maxTokens')?.value || 4096),
        topP: parseFloat(document.getElementById('topP')?.value || 0.9),
    };

    if (App.attachedImage) {
        req.imageBase64 = App.attachedImage.base64;
        req.imageMimeType = App.attachedImage.mimeType;
    }
    if (App.attachedDocument) {
        req.documentBase64 = App.attachedDocument.base64;
        req.documentName = App.attachedDocument.name;
    }

    try {
        const res = await fetch('/api/chat/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });

        if (!res.ok) throw new Error(await res.text());
        const data = await res.json();

        removeTypingIndicator();

        // Update conversation ID
        if (data.conversationId) {
            App.currentConversationId = data.conversationId;
            history.replaceState({}, '', `/chat?id=${data.conversationId}`);
        }

        // Append assistant message
        if (data.assistantMessage) {
            appendMessage(data.assistantMessage);
        }

        // Clear attachments
        clearAttachments();

        // Reload conversation list
        loadConversationsList();

        // Update token stats
        updateChatStats(data);

    } catch (e) {
        removeTypingIndicator();
        showToast(e.message || 'Erro ao enviar mensagem', 'error');
    } finally {
        App.isGenerating = false;
        if (sendBtn) sendBtn.disabled = false;
        scrollToBottom();
    }
}

function showTypingIndicator() {
    removeTypingIndicator();
    const area = document.getElementById('messagesArea');
    if (!area) return;
    const ind = document.createElement('div');
    ind.id = 'typingIndicator';
    ind.className = 'typing-indicator';
    ind.innerHTML = `
        <div class="message-avatar">🤖</div>
        <div class="typing-dots"><span></span><span></span><span></span></div>`;
    area.appendChild(ind);
}

function removeTypingIndicator() {
    document.getElementById('typingIndicator')?.remove();
}

function scrollToBottom() {
    const area = document.getElementById('messagesArea');
    if (area) setTimeout(() => { area.scrollTop = area.scrollHeight; }, 50);
}

function updateChatHeader(conv) {
    const titleEl = document.querySelector('.chat-title');
    if (titleEl) titleEl.textContent = conv.title || 'Conversa';
}

function updateChatStats(data) {
    const statsEl = document.querySelector('.chat-stats');
    if (statsEl && data.totalTokens) {
        statsEl.textContent = `${data.totalTokens} tokens · $${(+data.estimatedCost || 0).toFixed(4)}`;
    }
}

async function createNewChat() {
    try {
        const res = await fetch('/api/chat/new', { method: 'POST' });
        const conv = await res.json();
        window.location.href = `/chat?id=${conv.conversationId}`;
    } catch (e) {
        showToast('Erro ao criar conversa', 'error');
    }
}

async function deleteConversation(id) {
    if (!confirm('Excluir esta conversa?')) return;
    try {
        await fetch(`/api/chat/conversation/${id}`, { method: 'DELETE' });
        showToast('Conversa excluída', 'success');
        if (id === App.currentConversationId) {
            window.location.href = '/chat';
        } else {
            loadConversationsList();
        }
    } catch (e) {
        showToast('Erro ao excluir', 'error');
    }
}

function setSuggestion(text) {
    const ta = document.getElementById('messageInput');
    if (ta) { ta.value = text; ta.dispatchEvent(new Event('input')); ta.focus(); }
}

function selectModel(modelId, modelName) {
    App.currentModel = modelId;
    const btn = document.querySelector('.model-selector-btn');
    if (btn) {
        const nameEl = btn.querySelector('span');
        if (nameEl) nameEl.textContent = modelName;
    }
    closeModelDropdown();

    // If there's an open conversation, update its model
    if (App.currentConversationId) {
        fetch(`/api/chat/conversation/${App.currentConversationId}/model`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ modelId })
        }).catch(console.error);
    }
}

function toggleModelDropdown() {
    const panel = document.getElementById('modelDropdown');
    if (panel) panel.classList.toggle('open');
}

function closeModelDropdown() {
    document.getElementById('modelDropdown')?.classList.remove('open');
}

document.addEventListener('click', e => {
    if (!e.target.closest('.model-selector-dropdown')) closeModelDropdown();
});

function filterModelDropdown(query) {
    const q = query.toLowerCase();
    document.querySelectorAll('.model-dropdown-item').forEach(item => {
        const name = item.querySelector('.model-item-name')?.textContent.toLowerCase() || '';
        item.style.display = !q || name.includes(q) ? '' : 'none';
    });
}

// Settings Panel
function toggleSettings() {
    const panel = document.getElementById('settingsPanel');
    if (!panel) return;
    const isOpen = panel.style.width !== '0px' && panel.style.display !== 'none';
    if (!isOpen) { panel.style.display = 'flex'; panel.style.width = '320px'; }
    else { panel.style.display = 'none'; }
}

function closeSettings() {
    const panel = document.getElementById('settingsPanel');
    if (panel) panel.style.display = 'none';
}

function setSystemPrompt(text) {
    const ta = document.getElementById('systemPrompt');
    if (ta) ta.value = text;
}

// Attach Image
function triggerAttachImage() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = async e => {
        const file = e.target.files[0];
        if (!file) return;
        const base64 = await toBase64(file);
        App.attachedImage = { base64, mimeType: file.type, name: file.name };
        showAttachedPreview('image', file.name);
    };
    input.click();
}

// Attach Document
function triggerAttachDocument() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.pdf,.txt,.doc,.docx,.csv,.json';
    input.onchange = async e => {
        const file = e.target.files[0];
        if (!file) return;
        const base64 = await toBase64(file);
        App.attachedDocument = { base64, name: file.name, mimeType: file.type };
        showAttachedPreview('document', file.name);
    };
    input.click();
}

function showAttachedPreview(type, name) {
    const preview = document.getElementById('attachedPreview');
    if (!preview) return;
    preview.innerHTML = `
        <div class="attached-item">
            <i class="bi ${type === 'image' ? 'bi-image' : 'bi-file-earmark-text'} attached-icon"></i>
            <span>${escHtml(name)}</span>
            <button class="remove-attached" onclick="clearAttachments()"><i class="bi bi-x"></i></button>
        </div>`;
}

function clearAttachments() {
    App.attachedImage = null;
    App.attachedDocument = null;
    const preview = document.getElementById('attachedPreview');
    if (preview) preview.innerHTML = '';
}

// Enhance Prompt
async function enhanceChatPrompt() {
    const ta = document.getElementById('messageInput');
    if (!ta || !ta.value.trim()) { showToast('Enter a prompt first', 'warning'); return; }

    const btn = document.getElementById('enhancePromptBtn');
    if (btn) { btn.disabled = true; btn.innerHTML = '<i class="bi bi-arrow-clockwise"></i> Enhancing…'; }

    try {
        const res = await fetch('/api/chat/enhance-prompt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt: ta.value.trim() })
        });
        const data = await res.json();
        if (data.enhanced) { ta.value = data.enhanced; ta.dispatchEvent(new Event('input')); showToast('Prompt enhanced!', 'success'); }
    } catch { showToast('Enhancement failed', 'error'); }
    finally {
        if (btn) { btn.disabled = false; btn.innerHTML = '<i class="bi bi-stars"></i> Enhance'; }
    }
}

// Export Chat
function exportChat() {
    const messages = document.querySelectorAll('.message-wrapper');
    if (!messages.length) { showToast('No messages to export', 'warning'); return; }
    let md = `# Chat Export\n\n**Date:** ${new Date().toLocaleString()}\n\n---\n\n`;
    messages.forEach(w => {
        if (w.classList.contains('user-side')) {
            md += `**You:** ${w.querySelector('.message-content')?.textContent || ''}\n\n`;
        } else {
            md += `**AI:** ${w.querySelector('.prose')?.innerText || ''}\n\n`;
        }
        md += '---\n\n';
    });
    const blob = new Blob([md], { type: 'text/markdown' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `chat-${Date.now()}.md`;
    a.click();
}

// Compare Modal
function openCompareModal() {
    const prompt = document.getElementById('messageInput')?.value.trim();
    if (prompt) { const cta = document.getElementById('comparePromptText'); if (cta) cta.value = prompt; }
    document.getElementById('compareModal')?.removeAttribute('style');
    document.getElementById('compareModal')?.style.setProperty ? null : null;
    const modal = document.getElementById('compareModal');
    if (modal) modal.style.display = 'flex';
}

function closeCompareModal() {
    const modal = document.getElementById('compareModal');
    if (modal) modal.style.display = 'none';
}

async function runCompare() {
    const prompt = document.getElementById('comparePromptText')?.value.trim();
    if (!prompt) { showToast('Enter a prompt to compare', 'warning'); return; }

    const checked = Array.from(document.querySelectorAll('.compare-model-check input:checked')).map(cb => cb.value);
    if (checked.length < 2) { showToast('Select at least 2 models', 'warning'); return; }

    const btn = document.getElementById('btnRunCompare');
    if (btn) { btn.disabled = true; btn.textContent = 'Comparing…'; }

    try {
        const res = await fetch('/api/chat/compare', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt, modelIds: checked })
        });
        const results = await res.json();
        renderCompareResults(results, checked);
    } catch (e) {
        showToast('Compare failed', 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.innerHTML = '<i class="bi bi-columns-gap"></i> Run Comparison'; }
    }
}

function renderCompareResults(results, modelIds) {
    const container = document.getElementById('compareResults');
    if (!container) return;
    container.style.display = 'block';

    const grid = container.querySelector('.compare-grid');
    if (!grid) return;

    grid.innerHTML = modelIds.map(mid => {
        const r = results[mid];
        if (!r) return '';
        return `
            <div class="compare-result-card">
                <div class="compare-result-header">
                    <span class="model-name">${escHtml(mid)}</span>
                    <div class="compare-result-meta">
                        <span>${r.inputTokens || 0}+${r.outputTokens || 0} tok</span>
                        <span>${((r.latencyMs || 0)/1000).toFixed(1)}s</span>
                    </div>
                </div>
                <div class="compare-result-content prose">${r.formattedContent || escHtml(r.content || r.error || '')}</div>
            </div>`;
    }).join('');
}

// Templates Modal (Chat)
function openTemplatesModal() {
    const modal = document.getElementById('templatesModal');
    if (modal) modal.style.display = 'flex';
    loadChatTemplates();
}

function closeTemplatesModal() {
    const modal = document.getElementById('templatesModal');
    if (modal) modal.style.display = 'none';
}

async function loadChatTemplates(category = '', search = '') {
    try {
        const params = new URLSearchParams({ type: 'TEXT' });
        if (category) params.set('category', category);
        if (search) params.set('search', search);
        const res = await fetch('/api/templates?' + params);
        const templates = await res.json();
        renderChatTemplates(templates);
    } catch (e) { console.error(e); }
}

function renderChatTemplates(templates) {
    const grid = document.getElementById('chatTemplatesGrid');
    if (!grid) return;
    if (!templates.length) { grid.innerHTML = '<p style="color:var(--text-muted);padding:20px;text-align:center">Nenhum template encontrado</p>'; return; }
    grid.innerHTML = templates.map(t => `
        <div class="template-item" onclick="useChatTemplate(${JSON.stringify(escHtml(t.content))})">
            <div class="template-icon">${t.icon || '✨'}</div>
            <div class="template-info">
                <span class="template-name">${escHtml(t.name)}</span>
                <span class="template-cat">${escHtml(t.category || '')}</span>
            </div>
        </div>`).join('');
}

function filterChatTemplates(category) {
    document.querySelectorAll('.cat-btn').forEach(b => b.classList.toggle('active', b.dataset.cat === category));
    loadChatTemplates(category === 'ALL' ? '' : category);
}

function searchChatTemplates(query) {
    loadChatTemplates('', query);
}

function useChatTemplate(content) {
    const ta = document.getElementById('messageInput');
    if (ta) { ta.value = content; ta.dispatchEvent(new Event('input')); ta.focus(); }
    closeTemplatesModal();
}

// Pending template cross-page
function checkPendingTemplate() {
    const t = localStorage.getItem('pendingTemplate');
    if (!t) return;
    localStorage.removeItem('pendingTemplate');
    const path = window.location.pathname;
    if (path.startsWith('/chat')) {
        const ta = document.getElementById('messageInput') || document.getElementById('videoPrompt') || document.getElementById('imagePrompt');
        if (ta) { ta.value = t; ta.dispatchEvent(new Event('input')); }
    }
}

function useTemplate(content, type) {
    if (type === 'TEXT') {
        localStorage.setItem('pendingTemplate', content);
        window.location.href = '/chat';
    } else if (type === 'IMAGE') {
        localStorage.setItem('pendingTemplate', content);
        window.location.href = '/image';
    } else if (type === 'VIDEO') {
        localStorage.setItem('pendingTemplate', content);
        window.location.href = '/video';
    }
}

// Toggle Conversation Panel
function toggleConvSidebar() {
    const panel = document.querySelector('.conv-sidebar');
    if (panel) panel.classList.toggle('collapsed');
}

// ─────────────────────────────────────────────────────
// IMAGE GENERATION
// ─────────────────────────────────────────────────────
function initImage() {
    // Tab switching for results
    loadImageHistory();

    // Wire up Generate button
    const btnGen = document.getElementById('btnGenerateImage');
    if (btnGen) btnGen.addEventListener('click', generateImage);

    // Wire up Enhance prompt button
    const btnEnhance = document.getElementById('btnEnhanceImagePrompt');
    if (btnEnhance) btnEnhance.addEventListener('click', enhanceImagePrompt);

    // Wire up char counter on imagePrompt textarea
    const taPrompt = document.getElementById('imagePrompt');
    if (taPrompt) {
        taPrompt.addEventListener('input', updatePromptCounter);
        updatePromptCounter();
    }

    // Wire up reference image file input
    const refInput = document.getElementById('referenceImage');
    if (refInput) refInput.addEventListener('change', e => { if (e.target.files[0]) handleRefImage(e.target.files[0]); });

    // Check pending template
    const pending = localStorage.getItem('pendingTemplate');
    if (pending) {
        localStorage.removeItem('pendingTemplate');
        const ta = document.getElementById('imagePrompt');
        if (ta) { ta.value = pending; ta.dispatchEvent(new Event('input')); }
    }

    // Reference image drag & drop
    const uploadArea = document.getElementById('refImageUploadArea');
    if (uploadArea) {
        uploadArea.addEventListener('dragover', e => { e.preventDefault(); uploadArea.style.borderColor = 'var(--purple)'; });
        uploadArea.addEventListener('dragleave', () => { uploadArea.style.borderColor = ''; });
        uploadArea.addEventListener('drop', e => {
            e.preventDefault();
            uploadArea.style.borderColor = '';
            const file = e.dataTransfer.files[0];
            if (file) handleRefImage(file);
        });
    }
}

function selectImageModel(modelId, cardEl) {
    document.querySelectorAll('.model-card-small').forEach(c => c.classList.remove('active'));
    cardEl.classList.add('active');
    const input = document.getElementById('selectedImageModel');
    if (input) input.value = modelId;
    updatePromptCounter();
}

function updatePromptCounter() {
    const ta = document.getElementById('imagePrompt');
    const counter = document.getElementById('promptCharCounter');
    if (!ta || !counter) return;
    const len = ta.value.length;
    const modelInput = document.getElementById('selectedImageModel');
    const isTitan = modelInput && modelInput.value.includes('titan');
    counter.classList.remove('warn', 'danger');
    if (isTitan && len > 512) {
        counter.classList.add('danger');
        counter.textContent = `${len} caracteres ⚠️ Titan Image: limite de 512 (será cortado)`;
    } else if (isTitan && len > 400) {
        counter.classList.add('warn');
        counter.textContent = `${len}/512 caracteres (Titan Image)`;
    } else if (len > 800) {
        counter.classList.add('warn');
        counter.textContent = `${len} caracteres`;
    } else {
        counter.textContent = `${len} caracteres`;
    }
}

function useImageTemplate(prompt) {
    const ta = document.getElementById('imagePrompt');
    if (ta) { ta.value = prompt; ta.dispatchEvent(new Event('input')); ta.focus(); }
}

function setResolution(w, h, btn) {
    document.querySelectorAll('.res-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const wIn = document.getElementById('imgWidth');
    const hIn = document.getElementById('imgHeight');
    if (wIn) wIn.value = w;
    if (hIn) hIn.value = h;
}

function triggerRefImageUpload() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = e => { if (e.target.files[0]) handleRefImage(e.target.files[0]); };
    input.click();
}

async function handleRefImage(file) {
    App.refImageBase64 = await toBase64(file);
    const preview = document.getElementById('refPreview');
    const uploadArea = document.querySelector('#img2imgUpload .upload-area');
    if (preview) {
        preview.style.display = 'block';
        const img = document.getElementById('refPreviewImg');
        if (img) img.src = `data:${file.type};base64,${App.refImageBase64}`;
    }
    if (uploadArea) uploadArea.style.display = 'none';
    const strengthRow = document.getElementById('imgStrengthRow');
    if (strengthRow) strengthRow.style.display = 'block';
}

function removeRefImage() {
    App.refImageBase64 = null;
    const preview = document.getElementById('refPreview');
    const uploadArea = document.querySelector('#img2imgUpload .upload-area');
    if (preview) preview.style.display = 'none';
    if (uploadArea) uploadArea.style.display = 'flex';
    const strengthRow = document.getElementById('imgStrengthRow');
    if (strengthRow) strengthRow.style.display = 'none';
}

const clearReferenceImage = removeRefImage;

async function generateImage() {
    const prompt = document.getElementById('imagePrompt')?.value.trim();
    if (!prompt) { showToast('Digite um prompt para a imagem', 'warning'); return; }

    const modelId = document.getElementById('selectedImageModel')?.value;
    if (!modelId) { showToast('Selecione um modelo', 'warning'); return; }

    // Show generating state
    showGeneratingState();

    const request = {
        prompt,
        negativePrompt: document.getElementById('negativePrompt')?.value || '',
        modelId,
        width: parseInt(document.getElementById('imgWidth')?.value || 1024),
        height: parseInt(document.getElementById('imgHeight')?.value || 1024),
        numberOfImages: parseInt(document.getElementById('numImages')?.value || 1),
        cfgScale: parseFloat(document.getElementById('cfgScale')?.value || 7),
        style: document.getElementById('stylePreset')?.value || '',
        quality: document.getElementById('imageQuality')?.value || 'standard',
        seed: parseInt(document.getElementById('imgSeed')?.value || 0),
        referenceImageBase64: App.refImageBase64 || null,
        imageStrength: parseFloat(document.getElementById('imgStrength')?.value || 0.5),
    };

    // Disable generate button
    const btn = document.getElementById('btnGenerateImage');
    if (btn) btn.disabled = true;

    try {
        const res = await fetch('/api/image/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });

        if (!res.ok) throw new Error(await res.text());
        const data = await res.json();

        showGeneratedImages(data);
        loadImageHistory();
        const n = data.base64Images?.length || 1;
        showToast(`${n} imagem${n > 1 ? 'ns' : ''} gerada${n > 1 ? 's' : ''} com sucesso!`, 'success');

    } catch (e) {
        showToast(e.message || 'Falha ao gerar imagem', 'error');
        showPlaceholder();
    } finally {
        const btn2 = document.getElementById('btnGenerateImage');
        if (btn2) btn2.disabled = false;
    }
}

function toggleAccordion(id) {
    const el = document.getElementById(id);
    if (!el) return;
    const isOpen = el.style.display !== 'none';
    el.style.display = isOpen ? 'none' : 'block';
    const toggle = el.previousElementSibling;
    if (toggle) {
        const icon = toggle.querySelector('.acc-icon');
        if (icon) icon.style.transform = isOpen ? '' : 'rotate(180deg)';
    }
}

async function enhanceImagePrompt() {
    const ta = document.getElementById('imagePrompt');
    if (!ta || !ta.value.trim()) { showToast('Digite um prompt primeiro', 'warning'); return; }
    const btn = document.getElementById('btnEnhanceImagePrompt');
    if (btn) { btn.disabled = true; btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Melhorando...'; }
    try {
        const res = await fetch('/api/chat/enhance-prompt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt: ta.value, purpose: 'image generation' })
        });
        const data = await res.json();
        if (data.enhanced) { ta.value = data.enhanced; ta.dispatchEvent(new Event('input')); }
    } catch (e) {
        showToast('Erro ao melhorar prompt', 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.innerHTML = '<i class="bi bi-magic"></i> Melhorar'; }
    }
}

function showGeneratingState() {
    const resultArea = document.getElementById('resultArea');
    if (!resultArea) return;
    resultArea.innerHTML = `
        <div class="generating-state">
            <div class="img-loading-anim"><div class="img-progress-bar" id="imgProgressBar"></div></div>
            <p class="generating-text">✨ Criando sua obra de arte…</p>
            <div class="generation-steps">
                <span class="step active" id="step1">🎨 Interpretando o prompt</span>
                <span class="step" id="step2">🔮 Gerando pixels</span>
                <span class="step" id="step3">✨ Adicionando detalhes</span>
                <span class="step" id="step4">🖼 Finalizando</span>
            </div>
        </div>`;

    let step = 1;
    const stepInterval = setInterval(() => {
        step++;
        if (step > 4) { clearInterval(stepInterval); return; }
        document.querySelectorAll('.step').forEach(s => s.classList.remove('active'));
        const s = document.getElementById(`step${step}`);
        if (s) s.classList.add('active');
    }, 1200);

    App._stepInterval = stepInterval;
}

function showPlaceholder() {
    const resultArea = document.getElementById('resultArea');
    if (!resultArea) return;
    if (App._stepInterval) clearInterval(App._stepInterval);
    resultArea.innerHTML = `
        <div class="result-placeholder">
            <div class="img-canvas-placeholder"><i class="bi bi-image"></i></div>
            <h3>Pronto para criar</h3>
            <p>Configure as opções e clique em Gerar Imagem</p>
        </div>`;
}

function showGeneratedImages(data) {
    const resultArea = document.getElementById('resultArea');
    if (!resultArea) return;
    if (App._stepInterval) clearInterval(App._stepInterval);

    const imgs = data.base64Images || [];
    if (!imgs.length) { showPlaceholder(); return; }

    let html = `<div class="generated-images-grid">`;
    imgs.forEach((b64, i) => {
        html += `
            <div class="generated-img-item">
                <img src="data:image/png;base64,${b64}" alt="Generated ${i+1}" loading="lazy">
                <div class="img-item-overlay">
                    <button class="img-overlay-btn" onclick="openLightbox('${data.id}', ${i})" title="Ver em tela cheia"><i class="bi bi-arrows-fullscreen"></i></button>
                    <button class="img-overlay-btn" onclick="downloadImageById('${data.id}', ${i})" title="Baixar"><i class="bi bi-download"></i></button>
                    <button class="img-overlay-btn" onclick="toggleFavorite('${data.id}')" title="Favoritar"><i class="bi bi-heart"></i></button>
                </div>
            </div>`;
    });
    html += `</div>
        <div class="generation-meta">
            <span><i class="bi bi-cpu"></i> ${escHtml(data.modelId || '')}</span>
            <span><i class="bi bi-grid"></i> ${data.width}×${data.height}</span>
            <span><i class="bi bi-images"></i> ${imgs.length} imagem${imgs.length > 1 ? 'ns' : ''}</span>
            ${data.generationTimeMs ? `<span><i class="bi bi-clock"></i> ${(data.generationTimeMs/1000).toFixed(1)}s</span>` : ''}
        </div>`;
    resultArea.innerHTML = html;

    // Cache for lightbox
    App._lastGeneration = data;
}

// Lightbox
function openLightbox(genId, idx) {
    const gen = App._lastGeneration;
    if (!gen) return;
    const b64 = gen.base64Images[idx];
    if (!b64) return;

    App._lightboxGenId = genId;
    App._lightboxIdx = idx;

    const lb = document.getElementById('lightbox');
    if (!lb) return;

    const img = document.getElementById('lightboxImg');
    if (img) img.src = `data:image/png;base64,${b64}`;
    const prompt = document.getElementById('lightboxPrompt');
    if (prompt) prompt.textContent = gen.prompt;
    const meta = document.getElementById('lightboxMeta');
    if (meta) meta.innerHTML = `<span>${escHtml(gen.modelId)}</span><span>${gen.width}×${gen.height}</span>`;

    lb.style.display = 'flex';
}

function closeLightbox() {
    const lb = document.getElementById('lightbox');
    if (lb) lb.style.display = 'none';
}

function downloadLightboxImage() {
    const img = document.getElementById('lightboxImg');
    if (!img) return;
    const a = document.createElement('a');
    a.href = img.src;
    a.download = `ai-image-${Date.now()}.png`;
    a.click();
}

function useAsReference() {
    const img = document.getElementById('lightboxImg');
    if (!img) return;
    const base64 = img.src.split(',')[1];
    App.refImageBase64 = base64;
    const ref = document.getElementById('refImagePreview');
    const upload = document.getElementById('refImageUploadArea');
    if (ref) { ref.style.display = 'block'; const ri = ref.querySelector('img'); if (ri) ri.src = img.src; }
    if (upload) upload.style.display = 'none';
    closeLightbox();
    // Open ref accordion
    const refAcc = document.getElementById('refAccordion');
    if (refAcc && !refAcc.classList.contains('open')) { accordionToggle(refAcc.previousElementSibling); }
    showToast('Image set as reference!', 'success');
}

function downloadImageById(genId, idx) {
    fetch(`/api/image/${genId}/download?index=${idx}`)
        .then(r => r.blob())
        .then(blob => {
            const a = document.createElement('a');
            a.href = URL.createObjectURL(blob);
            a.download = `ai-image-${genId}-${idx}.png`;
            a.click();
        })
        .catch(() => {
            // Fallback: download from current generation
            const gen = App._lastGeneration;
            if (gen && gen.base64Images[idx]) {
                const a = document.createElement('a');
                a.href = `data:image/png;base64,${gen.base64Images[idx]}`;
                a.download = `ai-image-${Date.now()}.png`;
                a.click();
            }
        });
}

async function toggleFavorite(id) {
    try {
        await fetch(`/api/image/${id}/favorite`, { method: 'PATCH' });
        showToast('Favorito atualizado!', 'success');
        loadImageHistory();
    } catch { showToast('Erro', 'error'); }
}

async function deleteImage(id) {
    if (!confirm('Excluir esta imagem?')) return;
    try {
        await fetch(`/api/image/${id}`, { method: 'DELETE' });
        showToast('Imagem excluída', 'success');
        loadImageHistory();
    } catch { showToast('Erro ao excluir', 'error'); }
}

async function loadImageHistory() {
    try {
        const res = await fetch('/api/image/history');
        const history = await res.json();
        renderImageGallery(history);
    } catch (e) { console.error(e); }
}

function renderImageGallery(items) {
    const grid = document.getElementById('galleryGrid');
    if (!grid) return;

    if (!items.length) {
        grid.innerHTML = `<div class="gallery-empty"><i class="bi bi-images"></i><span>Nenhuma imagem ainda</span></div>`;
        return;
    }

    grid.innerHTML = items.map(item => {
        const first = item.base64Images?.[0];
        return `
            <div class="gallery-item">
                <div class="gallery-item-img">
                    ${first ? `<img src="data:image/png;base64,${first}" alt="${escHtml(item.prompt)}" loading="lazy">` : '<div style="background:var(--bg-tertiary);height:100%;display:flex;align-items:center;justify-content:center"><i class="bi bi-image" style="font-size:2rem;color:var(--text-muted)"></i></div>'}
                    <div class="gallery-overlay">
                        <button class="gallery-action-btn" onclick="downloadImageById('${item.id}', 0)" title="Baixar"><i class="bi bi-download"></i></button>
                        <button class="gallery-action-btn ${item.favorite ? 'text-danger' : ''}" onclick="toggleFavorite('${item.id}')" title="Favoritar"><i class="bi bi-heart${item.favorite ? '-fill' : ''}"></i></button>
                        <button class="gallery-action-btn danger" onclick="deleteImage('${item.id}')" title="Excluir"><i class="bi bi-trash"></i></button>
                    </div>
                </div>
                <div class="gallery-item-info">
                    <span class="gallery-prompt">${escHtml(item.prompt)}</span>
                    <span class="gallery-model">${escHtml(item.modelId || '')}</span>
                </div>
            </div>`;
    }).join('');
}

function filterGallery(filter) {
    document.querySelectorAll('.gallery-filter').forEach(b => b.classList.toggle('active', b.dataset.filter === filter));
    loadImageHistory();
}



// Accordion
function accordionToggle(btn) {
    const content = btn.nextElementSibling;
    if (!content) return;
    const isOpen = btn.classList.contains('open');
    btn.classList.toggle('open', !isOpen);
    if (isOpen) {
        content.style.display = 'none';
    } else {
        content.style.display = 'flex';
    }
}

// ─────────────────────────────────────────────────────
// VIDEO GENERATION
// ─────────────────────────────────────────────────────
function initVideo() {
    loadVideoHistory();

    // Check pending template
    const pending = localStorage.getItem('pendingTemplate');
    if (pending) {
        localStorage.removeItem('pendingTemplate');
        const ta = document.getElementById('videoPrompt');
        if (ta) { ta.value = pending; ta.dispatchEvent(new Event('input')); }
    }

    // Generate button
    const btnGen = document.getElementById('btnGenerateVideo');
    if (btnGen) btnGen.addEventListener('click', generateVideo);

    // Enhance button
    const btnEnhance = document.getElementById('btnEnhanceVideoPrompt');
    if (btnEnhance) btnEnhance.addEventListener('click', enhanceVideoPrompt);

    // Reference image file input
    const refInput = document.getElementById('videoRefImage');
    if (refInput) {
        refInput.addEventListener('change', async e => {
            const file = e.target.files[0];
            if (!file) return;
            App.refVideoImageBase64 = await toBase64(file);
            const preview = document.getElementById('videoRefPreview');
            const img = document.getElementById('videoRefPreviewImg');
            if (img) img.src = 'data:' + file.type + ';base64,' + App.refVideoImageBase64;
            if (preview) preview.style.display = 'flex';
            showToast('Imagem de referência anexada!', 'success');
        });
    }

    // Init counter
    updatePromptCounter();
}

function updatePromptCounter() {
    const ta = document.getElementById('videoPrompt');
    const counter = document.getElementById('promptCharCount');
    if (!ta || !counter) return;
    const len = ta.value.length;
    counter.textContent = len;
    counter.style.color = len >= 512 ? 'var(--red, #e53e3e)'
        : len >= 490 ? 'var(--warning, #d97706)'
        : 'var(--text-muted)';
}

function useVideoTemplate(text) {
    const ta = document.getElementById('videoPrompt');
    if (ta) { ta.value = text; updatePromptCounter(); ta.focus(); }
}

async function generateVideo() {
    const prompt = document.getElementById('videoPrompt')?.value.trim();
    if (!prompt) { showToast('Digite um prompt para o vídeo', 'warning'); return; }

    const request = {
        prompt,
        modelId: 'amazon.nova-reel-v1:0',
        durationSeconds: 6,
        resolution: '1280x720',
        referenceImageBase64: App.refVideoImageBase64 || null,
    };

    const btn = document.getElementById('btnGenerateVideo');
    if (btn) { btn.disabled = true; btn.innerHTML = '<i class="bi bi-hourglass-split"></i> <span>Gerando…</span>'; }

    try {
        const res = await fetch('/api/video/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });

        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || await res.text());
        }
        const data = await res.json();

        showToast('Geração de vídeo iniciada! Isso leva de 2 a 8 minutos.', 'success');
        addToQueue({ ...data, prompt });
        startPollingVideo(data.id);

    } catch (e) {
        showToast(e.message || 'Falha ao iniciar geração de vídeo', 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.innerHTML = '<i class="bi bi-play-circle-fill"></i> <span>Gerar Vídeo</span>'; }
    }
}

function addToQueue(video) {
    const list = document.getElementById('queueList');
    if (!list) return;
    const empty = list.querySelector('.queue-empty');
    if (empty) empty.remove();

    const item = document.createElement('div');
    item.className = 'queue-item';
    item.id = `queue-${video.id}`;
    item.innerHTML = `
        <div class="queue-status-badge processing">
            <div class="spinner-small"></div> Processando
        </div>
        <p class="queue-prompt">${escHtml(video.prompt)}</p>
        <span class="queue-time">Iniciado agora</span>`;
    list.prepend(item);
}

function removeFromQueue(videoId) {
    document.getElementById(`queue-${videoId}`)?.remove();
    const list = document.getElementById('queueList');
    if (list && !list.children.length) {
        list.innerHTML = `<div class="queue-empty"><i class="bi bi-collection-play"></i><span>Nenhum processamento ativo</span></div>`;
    }
}

function startPollingVideo(videoId) {
    if (App.videoPollers[videoId]) return;
    App.videoPollers[videoId] = setInterval(() => pollVideoStatus(videoId), 5000);
}

async function pollVideoStatus(videoId) {
    try {
        const res = await fetch(`/api/video/${videoId}/status`);
        const video = await res.json();

        if (video.status === 'COMPLETED' || video.status === 'FAILED') {
            clearInterval(App.videoPollers[videoId]);
            delete App.videoPollers[videoId];
            removeFromQueue(videoId);
            loadVideoHistory();

            if (video.status === 'COMPLETED') {
                showToast('Geração de vídeo concluída! 🎬', 'success');
            } else {
                showToast(`Vídeo falhou: ${video.errorMessage || 'Erro desconhecido'}`, 'error');
            }
        }
    } catch (e) {
        console.error('Poll error:', e);
    }
}

async function loadVideoHistory() {
    try {
        const res = await fetch('/api/video/history');
        const videos = await res.json();
        renderVideoCards(videos);
    } catch (e) { console.error(e); }
}

// Returns a human-readable countdown until 60 min after completedAt
function videoExpiryLabel(completedAtStr) {
    if (!completedAtStr) return null;
    const expiresAt = new Date(new Date(completedAtStr).getTime() + 60 * 60 * 1000);
    const remaining = expiresAt - Date.now();
    if (remaining <= 0) return '⏰ Expirando...';
    const mins = Math.floor(remaining / 60000);
    if (mins < 60) return `⏳ Expira em ${mins} min`;
    const h = Math.floor(mins / 60), m = mins % 60;
    return `⏳ Expira em ${h}h${m > 0 ? ` ${m}min` : ''}`;
}

function renderVideoCards(videos) {
    const container = document.getElementById('videoCards');
    if (!container) return;

    // DOWNLOADED status is already filtered server-side; guard here too
    const finished = videos.filter(v => v.status === 'COMPLETED' || v.status === 'FAILED');
    const pending   = videos.filter(v => v.status === 'PENDING'   || v.status === 'PROCESSING');

    // Start pollers for in-progress videos
    pending.forEach(v => startPollingVideo(v.id));

    if (!finished.length) {
        container.innerHTML = `
            <div class="video-empty">
                <i class="bi bi-film"></i>
                <h4>Nenhum vídeo gerado ainda</h4>
                <p>Configure o prompt e gere seu primeiro vídeo com IA</p>
            </div>`;
        return;
    }

    container.innerHTML = finished.map(v => {
        if (v.status === 'COMPLETED') {
            const expiry = videoExpiryLabel(v.completedAt);
            return `
                <div class="video-card" id="vcard-${v.id}">
                    <div class="video-card-player">
                        <i class="bi bi-play-circle-fill" style="font-size:3rem;color:var(--red)"></i>
                        <div class="video-card-overlay">
                            <span class="video-badge completed">COMPLETED</span>
                            ${expiry ? `<span class="video-badge expiry-badge" title="Vídeos são removidos automaticamente do servidor após 60 minutos">${expiry}</span>` : ''}
                        </div>
                    </div>
                    <div class="video-card-body">
                        <p class="video-card-prompt">${escHtml(v.prompt)}</p>
                        <div class="video-card-actions">
                            ${v.s3Uri ? `<button class="btn-video-action" id="btn-dl-${v.id}" onclick="downloadVideo('${v.id}')"><i class="bi bi-download"></i> Baixar Vídeo</button>` : ''}
                            <button onclick="deleteVideo('${v.id}')" title="Excluir e remover do servidor"><i class="bi bi-trash"></i></button>
                        </div>
                    </div>
                </div>`;
        } else {
            return `
                <div class="video-card failed" id="vcard-${v.id}">
                    <div class="video-card-player error-card"><i class="bi bi-exclamation-triangle-fill"></i></div>
                    <div class="video-card-body">
                        <p class="video-card-prompt">${escHtml(v.prompt)}</p>
                        <p class="error-msg">${escHtml(v.errorMessage || 'Generation failed')}</p>
                        <div class="video-card-actions">
                            <button onclick="deleteVideo('${v.id}')" title="Remover"><i class="bi bi-trash"></i></button>
                        </div>
                    </div>
                </div>`;
        }
    }).join('');
}

// Smooth animated removal of a video card. Shows empty-state if none remain.
function removeVideoCard(id) {
    const card = document.getElementById(`vcard-${id}`);
    if (!card) return;
    card.style.transition = 'opacity 0.35s ease, transform 0.35s ease';
    card.style.opacity = '0';
    card.style.transform = 'scale(0.9)';
    setTimeout(() => {
        card.remove();
        const container = document.getElementById('videoCards');
        if (container && !container.querySelector('.video-card')) {
            container.innerHTML = `
                <div class="video-empty">
                    <i class="bi bi-film"></i>
                    <h4>Nenhum vídeo gerado ainda</h4>
                    <p>Configure o prompt e gere seu primeiro vídeo com IA</p>
                </div>`;
        }
    }, 380);
}

async function downloadVideo(id) {
    const btn = document.getElementById(`btn-dl-${id}`);
    if (btn) { btn.disabled = true; btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Preparando...'; }
    try {
        const res = await fetch(`/api/video/${id}/download-url`);
        const data = await res.json();
        if (data.url) {
            // Trigger browser download
            const a = document.createElement('a');
            a.href = data.url;
            a.download = `video-${id}.mp4`;
            a.target = '_blank';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);

            // Remove card instantly – server already scheduled S3 cleanup
            removeVideoCard(id);
            showToast('Download iniciado! O arquivo será removido do servidor em instantes.', 'success');
        } else {
            if (btn) { btn.disabled = false; btn.innerHTML = '<i class="bi bi-download"></i> Baixar Vídeo'; }
            showToast(data.error || 'Erro ao gerar link de download', 'error');
        }
    } catch (e) {
        if (btn) { btn.disabled = false; btn.innerHTML = '<i class="bi bi-download"></i> Baixar Vídeo'; }
        showToast('Erro ao baixar vídeo', 'error');
    }
}

async function deleteVideo(id) {
    if (!confirm('Excluir este vídeo? O arquivo será removido do servidor imediatamente.')) return;
    try {
        const res = await fetch(`/api/video/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
            removeVideoCard(id);
            showToast('Vídeo excluído e removido do servidor.', 'success');
        }
    } catch {
        showToast('Erro ao excluir vídeo', 'error');
    }
}

function triggerVideoRefImage() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = async e => {
        const file = e.target.files[0];
        if (!file) return;
        App.refVideoImageBase64 = await toBase64(file);
        showToast('Reference image attached!', 'success');
    };
    input.click();
}

async function enhanceVideoPrompt() {
    const ta = document.getElementById('videoPrompt');
    if (!ta || !ta.value.trim()) { showToast('Digite um prompt primeiro', 'warning'); return; }
    const btn = document.getElementById('btnEnhanceVideoPrompt');
    if (btn) { btn.disabled = true; btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Melhorando...'; }
    try {
        const res = await fetch('/api/chat/enhance-prompt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt: ta.value, purpose: 'video generation' })
        });
        const data = await res.json();
        if (data.enhanced) { ta.value = data.enhanced; ta.dispatchEvent(new Event('input')); }
    } catch (e) {
        showToast('Erro ao melhorar prompt', 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.innerHTML = '<i class="bi bi-magic"></i> Melhorar'; }
    }
}

function clearVideoRef() {
    App.refVideoImageBase64 = null;
    const preview = document.getElementById('videoRefPreview');
    const img = document.getElementById('videoRefPreviewImg');
    const input = document.getElementById('videoRefImage');
    if (preview) preview.style.display = 'none';
    if (img) img.src = '';
    if (input) input.value = '';
    showToast('Imagem de referência removida', 'info');
}

function retryVideo(prompt) {
    const ta = document.getElementById('videoPrompt');
    if (ta) { ta.value = prompt; ta.dispatchEvent(new Event('input')); ta.focus(); }
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ─────────────────────────────────────────────────────
// HISTORY PAGE
// ─────────────────────────────────────────────────────
function initHistory() {
    // Tab switching already handled by HTML onclick, ensure first tab visible
    document.querySelectorAll('.tab-pane').forEach((p, i) => {
        p.style.display = i === 0 ? '' : 'none';
    });
}

function switchTab(tabId, btn) {
    document.querySelectorAll('.tab-pane').forEach(p => p.style.display = 'none');
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    const pane = document.getElementById(tabId);
    if (pane) pane.style.display = '';
    if (btn) btn.classList.add('active');
}

async function clearAllConversations() {
    if (!confirm('Delete ALL conversations? This cannot be undone.')) return;
    try {
        await fetch('/api/history/conversations', { method: 'DELETE' });
        showToast('All conversations deleted', 'success');
        setTimeout(() => location.reload(), 1000);
    } catch { showToast('Error', 'error'); }
}

// ─────────────────────────────────────────────────────
// TEMPLATES PAGE
// ─────────────────────────────────────────────────────
function initTemplatesPage() {
    // Filter state
    App.templateFilter = { type: '', category: '', search: '' };
}

function filterTemplateType(type, btn) {
    document.querySelectorAll('[data-type-filter]').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    App.templateFilter = { ...App.templateFilter, type };
}

function searchTemplates(query) {
    App.templateFilter = { ...App.templateFilter, search: query };
}

function openCreateTemplateModal() {
    const modal = document.getElementById('createTemplateModal');
    if (modal) modal.style.display = 'flex';
}

function closeCreateTemplateModal() {
    const modal = document.getElementById('createTemplateModal');
    if (modal) modal.style.display = 'none';
    document.getElementById('createTemplateForm')?.reset();
}

async function saveTemplate() {
    const name = document.getElementById('newTplName')?.value.trim();
    const content = document.getElementById('newTplContent')?.value.trim();
    const type = document.getElementById('newTplType')?.value;
    const category = document.getElementById('newTplCategory')?.value;
    const icon = document.getElementById('newTplIcon')?.value || '✨';
    const description = document.getElementById('newTplDescription')?.value.trim();

    if (!name || !content) { showToast('Name and content are required', 'warning'); return; }

    try {
        await fetch('/api/templates', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, content, type, category, icon, description })
        });
        showToast('Template saved!', 'success');
        closeCreateTemplateModal();
        setTimeout(() => location.reload(), 800);
    } catch { showToast('Error saving template', 'error'); }
}

async function deleteTemplate(id) {
    if (!confirm('Delete this template?')) return;
    try {
        await fetch(`/api/templates/${id}`, { method: 'DELETE' });
        showToast('Template deleted', 'success');
        document.getElementById(`tpl-${id}`)?.remove();
    } catch { showToast('Error', 'error'); }
}

async function toggleTemplateFavorite(id) {
    try {
        await fetch(`/api/templates/${id}/favorite`, { method: 'PATCH' });
        const icon = document.querySelector(`#tpl-${id} .tc-favorite`);
        if (icon) icon.textContent = icon.textContent === '⭐' ? '☆' : '⭐';
    } catch { showToast('Error', 'error'); }
}

function useTemplateFromPage(content, type) {
    useTemplate(content, type);
}

// Close modals on backdrop click
document.addEventListener('click', e => {
    if (e.target.id === 'compareModal') closeCompareModal();
    if (e.target.id === 'templatesModal') closeTemplatesModal();
    if (e.target.id === 'lightbox') closeLightbox();
    if (e.target.id === 'createTemplateModal') closeCreateTemplateModal();
});

// ESC closes modals
document.addEventListener('keydown', e => {
    if (e.key === 'Escape') {
        closeCompareModal();
        closeTemplatesModal();
        closeLightbox();
        closeCreateTemplateModal();
        closeSettings();
    }
});
