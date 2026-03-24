/* ==========================================
   INDEX SCRIPTS - PÁGINA PRINCIPAL
   JavaScript otimizado para a página inicial
   ========================================== */

class IndexApp {
    constructor() {
        this.initializeApp();
    }

    initializeApp() {
        this.setupThemeToggle();
        this.setupScrollEffects();
        this.setupCardAnimations();
        this.setupRippleEffect();
        this.setupPremiumPillars();
        this.setupIOSFixes();
        this.initializeOnLoad();
    }

    initializeOnLoad() {
        // Aguarda o DOM carregar completamente
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                this.startInitialAnimations();
                this.setupIntersectionObserver();
            });
        } else {
            this.startInitialAnimations();
            this.setupIntersectionObserver();
        }
    }

    startInitialAnimations() {
        // Anima elementos do hero
        const heroElements = document.querySelectorAll('.hero-title, .hero-subtitle, .hero-verse');
        heroElements.forEach((el, index) => {
            el.style.opacity = '0';
            el.style.transform = 'translateY(30px)';
            setTimeout(() => {
                el.style.transition = 'all 0.8s ease';
                el.style.opacity = '1';
                el.style.transform = 'translateY(0)';
            }, index * 200);
        });
    }

    setupThemeToggle() {
        const themeToggle = document.getElementById('theme-toggle');
        
        // Aplicar tema salvo
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme === 'light') {
            document.body.classList.add('light');
            if (themeToggle) {
                themeToggle.innerHTML = '<i class="fas fa-moon"></i>';
            }
        }

        // Toggle do tema
        if (themeToggle) {
            themeToggle.addEventListener('click', () => {
                document.body.classList.toggle('light');
                const isLight = document.body.classList.contains('light');
                
                // Salvar preferência
                localStorage.setItem('theme', isLight ? 'light' : 'dark');
                
                // Atualizar ícone
                themeToggle.innerHTML = isLight ? '<i class="fas fa-moon"></i>' : '<i class="fas fa-sun"></i>';
                
                // Adicionar efeito visual
                themeToggle.style.transform = 'rotate(180deg)';
                setTimeout(() => {
                    themeToggle.style.transform = 'rotate(0deg)';
                }, 300);
            });
        }
    }

    setupScrollEffects() {
        let lastScrollTop = 0;
        const header = document.querySelector('.modern-header');

        window.addEventListener('scroll', () => {
            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
            
            // Efeito de blur no header
            if (scrollTop > 50 && header) {
                header.classList.add('scrolled');
            } else if (header) {
                header.classList.remove('scrolled');
            }

            lastScrollTop = scrollTop;
        });

        // Parallax suave para elementos do hero
        window.addEventListener('scroll', () => {
            const scrolled = window.pageYOffset;
            const parallaxElements = document.querySelectorAll('.hero-content');
            
            parallaxElements.forEach(element => {
                const speed = 0.3;
                element.style.transform = `translateY(${scrolled * speed}px)`;
            });
        });
    }

    setupCardAnimations() {
        // Adiciona animações hover mais fluidas
        const cards = document.querySelectorAll('.month-card, .sermon-card');
        
        cards.forEach(card => {
            card.addEventListener('mouseenter', () => {
                card.style.transition = 'all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
            });
            
            card.addEventListener('mouseleave', () => {
                card.style.transition = 'all 0.3s ease';
            });
        });
    }

    setupRippleEffect() {
        // Adiciona efeito ripple aos cartões
        const cards = document.querySelectorAll('.sermon-card, .modern-btn');
        
        cards.forEach(card => {
            card.addEventListener('click', (e) => {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                
                const ripple = document.createElement('div');
                ripple.style.position = 'absolute';
                ripple.style.borderRadius = '50%';
                ripple.style.background = 'rgba(255, 255, 255, 0.3)';
                ripple.style.pointerEvents = 'none';
                ripple.style.transform = 'scale(0)';
                ripple.style.animation = 'ripple 0.6s linear';
                ripple.style.left = x + 'px';
                ripple.style.top = y + 'px';
                ripple.style.width = ripple.style.height = '0px';
                
                card.style.position = 'relative';
                card.appendChild(ripple);
                
                setTimeout(() => {
                    ripple.remove();
                }, 600);
            });
        });
        
        // CSS para animação do ripple
        if (!document.querySelector('#ripple-styles')) {
            const style = document.createElement('style');
            style.id = 'ripple-styles';
            style.textContent = `
                @keyframes ripple {
                    to {
                        transform: scale(4);
                        opacity: 0;
                        width: 100px;
                        height: 100px;
                        margin-left: -50px;
                        margin-top: -50px;
                    }
                }
            `;
            document.head.appendChild(style);
        }
    }

    setupIntersectionObserver() {
        // Observer para animações de entrada
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const element = entry.target;
                    
                    // Anima cartões de mês
                    if (element.classList.contains('month-card')) {
                        element.style.opacity = '0';
                        element.style.transform = 'translateY(50px)';
                        setTimeout(() => {
                            element.style.transition = 'all 0.8s ease';
                            element.style.opacity = '1';
                            element.style.transform = 'translateY(0)';
                        }, 100);
                    }
                    
                    // Anima cartões de sermão com delay escalonado
                    if (element.classList.contains('sermon-card')) {
                        const cards = Array.from(element.parentNode.children);
                        const index = cards.indexOf(element);
                        
                        element.style.opacity = '0';
                        element.style.transform = 'translateY(30px)';
                        setTimeout(() => {
                            element.style.transition = 'all 0.6s ease';
                            element.style.opacity = '1';
                            element.style.transform = 'translateY(0)';
                        }, index * 100);
                    }
                    
                    observer.unobserve(element);
                }
            });
        }, {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        });

        // Observa elementos que devem ser animados
        const animatedElements = document.querySelectorAll('.month-card, .sermon-card');
        animatedElements.forEach(el => observer.observe(el));
    }

    setupIOSFixes() {
        // Detecção de iOS
        const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);
        
        if (isIOS) {
            document.body.classList.add('ios-device');
            
            // Fix para viewport height em iOS
            const setViewportHeight = () => {
                const vh = window.innerHeight * 0.01;
                document.documentElement.style.setProperty('--vh', `${vh}px`);
            };
            
            setViewportHeight();
            window.addEventListener('resize', setViewportHeight);
            window.addEventListener('orientationchange', () => {
                setTimeout(setViewportHeight, 100);
            });
        }
    }

    // Métodos utilitários
    addSmoothScrolling() {
        // Adiciona scroll suave para âncoras
        const links = document.querySelectorAll('a[href^="#"]');
        
        links.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const targetId = link.getAttribute('href').substring(1);
                const targetElement = document.getElementById(targetId);
                
                if (targetElement) {
                    targetElement.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            });
        });
    }

    preloadImages() {
        // Preload de imagens importantes para melhor performance
        const imageUrls = [
            // Adicione URLs de imagens que precisam ser carregadas rapidamente
        ];
        
        imageUrls.forEach(url => {
            const img = new Image();
            img.src = url;
        });
    }

    setupPerformanceOptimizations() {
        // Lazy loading para elementos pesados
        if ('IntersectionObserver' in window) {
            const lazyElements = document.querySelectorAll('[data-lazy]');
            
            const lazyObserver = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const element = entry.target;
                        const src = element.getAttribute('data-lazy');
                        
                        if (element.tagName === 'IMG') {
                            element.src = src;
                        } else {
                            element.style.backgroundImage = `url(${src})`;
                        }
                        
                        element.removeAttribute('data-lazy');
                        lazyObserver.unobserve(element);
                    }
                });
            });
            
            lazyElements.forEach(el => lazyObserver.observe(el));
        }
    }

    // Método para debug (remover em produção)
    debug() {
        console.log('IndexApp initialized');
        console.log('Theme:', document.body.classList.contains('light') ? 'light' : 'dark');
        console.log('Cards found:', document.querySelectorAll('.sermon-card').length);
    }

    // Nova funcionalidade para Pilares Premium
    setupPremiumPillars() {
        const pillarCards = document.querySelectorAll('.premium-pillar-card');
        
        pillarCards.forEach(card => {
            // Adicionar efeito de hover 3D suave
            card.addEventListener('mouseenter', (e) => {
                const rect = card.getBoundingClientRect();
                const centerX = rect.left + rect.width / 2;
                const centerY = rect.top + rect.height / 2;
                
                card.style.transition = 'transform 0.4s cubic-bezier(0.4, 0, 0.2, 1)';
            });
            
            card.addEventListener('mousemove', (e) => {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                
                const centerX = rect.width / 2;
                const centerY = rect.height / 2;
                
                const rotateX = (y - centerY) / 20;
                const rotateY = (centerX - x) / 20;
                
                card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-8px)`;
            });
            
            card.addEventListener('mouseleave', () => {
                card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateY(0px)';
            });
            
            // Funcionalidade de clique para mostrar versículos
            card.addEventListener('click', () => {
                const verseRef = card.getAttribute('data-ref');
                const verseBlock = card.querySelector('.verse-block');
                
                if (verseBlock.classList.contains('d-none')) {
                    this.showPillarVerse(card, verseRef, verseBlock);
                } else {
                    this.hidePillarVerse(card, verseBlock);
                }
            });
        });
    }
    
    showPillarVerse(card, verseRef, verseBlock) {
        // Fechar outros versículos abertos
        document.querySelectorAll('.premium-pillar-card .verse-block').forEach(block => {
            if (!block.classList.contains('d-none') && block !== verseBlock) {
                block.classList.add('d-none');
                block.style.opacity = '0';
                block.style.transform = 'translateY(-20px)';
            }
        });
        
        // Buscar versículo (assumindo que window.Verses existe)
        const verse = window.Verses?.[verseRef] || `[${verseRef}] Versículo não encontrado.`;
        
        // Criar conteúdo do versículo
        verseBlock.innerHTML = `
            <div class="pillar-verse-content">
                <div class="verse-divider"></div>
                <p class="verse-text">${verse}</p>
                <p class="verse-reference">— ${verseRef}</p>
            </div>
        `;
        
        // Mostrar com animação
        verseBlock.classList.remove('d-none');
        verseBlock.style.opacity = '0';
        verseBlock.style.transform = 'translateY(-20px)';
        
        setTimeout(() => {
            verseBlock.style.transition = 'all 0.6s cubic-bezier(0.4, 0, 0.2, 1)';
            verseBlock.style.opacity = '1';
            verseBlock.style.transform = 'translateY(0)';
        }, 100);
        
        // Scroll suave para o cartão
        card.scrollIntoView({ 
            behavior: 'smooth', 
            block: 'center' 
        });
    }
    
    hidePillarVerse(card, verseBlock) {
        verseBlock.style.transition = 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)';
        verseBlock.style.opacity = '0';
        verseBlock.style.transform = 'translateY(-20px)';
        
        setTimeout(() => {
            verseBlock.classList.add('d-none');
        }, 400);
    }
}

// Inicialização da aplicação
const indexApp = new IndexApp();

// Exportar para uso global se necessário
window.IndexApp = IndexApp;
