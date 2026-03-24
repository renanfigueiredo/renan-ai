/**
 * SERMON SCRIPTS - ARQUIVO COMPARTILHADO
 * Scripts premium para todas as páginas de sermões
 */

class SermonApp {
  constructor() {
    this.init();
  }

  init() {
    this.setupVerseExpansion();
    this.setupAnimations();
    this.setupRippleEffects();
    this.setupIntersectionObserver();
  }

  /**
   * Sistema de expansão de versículos
   */
  setupVerseExpansion() {
    document.querySelectorAll('.verse-ref').forEach(ref => {
      ref.addEventListener('click', (e) => {
        e.preventDefault();
        const verseKey = ref.getAttribute('data-ref');
        let expanded = ref.parentNode.querySelector('.verse-expanded');
        
        if (expanded) {
          expanded.remove();
          return;
        }
        
        const verse = window.Verses?.[verseKey] || `[${verseKey}] Texto não encontrado.`;
        expanded = document.createElement('div');
        expanded.className = 'verse-expanded mt-3 p-3 rounded';
        expanded.style.background = 'rgba(255, 255, 255, 0.1)';
        expanded.style.borderLeft = '4px solid var(--neon-blue)';
        expanded.innerHTML = `<strong>${verseKey}:</strong><br>${verse}`;
        
        ref.insertAdjacentElement('afterend', expanded);
        
        // Animar entrada
        expanded.style.opacity = '0';
        expanded.style.transform = 'translateY(-20px)';
        setTimeout(() => {
          expanded.style.transition = 'all 0.3s ease';
          expanded.style.opacity = '1';
          expanded.style.transform = 'translateY(0)';
        }, 10);
      });
    });
  }

  /**
   * Sistema de animações suaves
   */
  setupAnimations() {
    // Adicionar CSS de animação dinamicamente
    const style = document.createElement('style');
    style.textContent = `
      @keyframes fadeInUp {
        from {
          opacity: 0;
          transform: translateY(30px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
      
      .sermon-section, .structure-card, .practice-card, .application-card {
        opacity: 0;
      }
    `;
    document.head.appendChild(style);
  }

  /**
   * Observer para animações na entrada
   */
  setupIntersectionObserver() {
    const observerOptions = {
      threshold: 0.1,
      rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.style.animation = 'fadeInUp 0.6s ease forwards';
          observer.unobserve(entry.target);
        }
      });
    }, observerOptions);

    // Observar elementos para animação
    document.querySelectorAll('.sermon-section, .structure-card, .practice-card, .application-card').forEach(el => {
      observer.observe(el);
    });
  }

  /**
   * Efeitos ripple nos cards
   */
  setupRippleEffects() {
    // Adicionar CSS do ripple
    const rippleStyle = document.createElement('style');
    rippleStyle.textContent = `
      @keyframes ripple {
        to {
          transform: scale(4);
          opacity: 0;
        }
      }
    `;
    document.head.appendChild(rippleStyle);

    // Aplicar efeito ripple
    document.querySelectorAll('.practice-card, .structure-card, .application-card, .question-item').forEach(card => {
      card.addEventListener('click', (e) => {
        this.createRipple(e, card);
      });
    });
  }

  /**
   * Criar efeito ripple
   */
  createRipple(event, element) {
    const ripple = document.createElement('span');
    const rect = element.getBoundingClientRect();
    const size = 60;
    const x = event.clientX - rect.left - size / 2;
    const y = event.clientY - rect.top - size / 2;
    
    ripple.style.cssText = `
      position: absolute;
      width: ${size}px;
      height: ${size}px;
      background: rgba(255, 255, 255, 0.3);
      border-radius: 50%;
      left: ${x}px;
      top: ${y}px;
      animation: ripple 0.6s linear;
      pointer-events: none;
    `;
    
    element.style.position = 'relative';
    element.style.overflow = 'hidden';
    element.appendChild(ripple);
    
    setTimeout(() => ripple.remove(), 600);
  }

  /**
   * Utilitário para smooth scroll
   */
  static smoothScrollTo(element) {
    element.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    });
  }

  /**
   * Utilitário para copiar texto
   */
  static copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
      // Feedback visual (opcional)
      console.log('Texto copiado!');
    });
  }
}

// Inicializar quando o DOM estiver carregado
document.addEventListener('DOMContentLoaded', () => {
  new SermonApp();
});

// Exportar para uso global se necessário
window.SermonApp = SermonApp;
