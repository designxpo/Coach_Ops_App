// CoachOps marketing site — interactions
(function () {
  'use strict';

  // --- Nav: solidify glass on scroll ---
  const nav = document.querySelector('.nav');
  const onScroll = () => {
    if (window.scrollY > 20) nav.classList.add('is-scrolled');
    else nav.classList.remove('is-scrolled');
  };
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  // --- Mobile menu toggle ---
  const burger = document.getElementById('burger');
  const menu = document.getElementById('mobileMenu');
  const closeMenu = () => {
    menu.hidden = true;
    burger.setAttribute('aria-expanded', 'false');
  };
  burger.addEventListener('click', () => {
    const open = burger.getAttribute('aria-expanded') === 'true';
    if (open) { closeMenu(); }
    else { menu.hidden = false; burger.setAttribute('aria-expanded', 'true'); }
  });
  menu.querySelectorAll('a').forEach((a) => a.addEventListener('click', closeMenu));

  // --- Scroll reveal ---
  const reveals = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window) {
    const io = new IntersectionObserver((entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting) {
          e.target.classList.add('in');
          io.unobserve(e.target);
        }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -60px 0px' });
    reveals.forEach((el, i) => {
      el.style.transitionDelay = Math.min(i % 4, 3) * 80 + 'ms';
      io.observe(el);
    });
  } else {
    reveals.forEach((el) => el.classList.add('in'));
  }

  // --- Subtle parallax / tilt on hero phones (pointer) ---
  const visual = document.querySelector('.hero__visual');
  if (visual && window.matchMedia('(pointer:fine)').matches &&
      !window.matchMedia('(prefers-reduced-motion:reduce)').matches) {
    visual.addEventListener('pointermove', (ev) => {
      const r = visual.getBoundingClientRect();
      const x = (ev.clientX - r.left) / r.width - 0.5;
      const y = (ev.clientY - r.top) / r.height - 0.5;
      visual.style.setProperty('--px', (x * 14).toFixed(2) + 'px');
      visual.style.setProperty('--py', (y * 14).toFixed(2) + 'px');
      visual.querySelectorAll('.phone').forEach((p, i) => {
        const depth = (i + 1) * 0.6;
        p.style.transform = '';
        p.style.translate = `${x * 10 * depth}px ${y * 10 * depth}px`;
      });
    });
    visual.addEventListener('pointerleave', () => {
      visual.querySelectorAll('.phone').forEach((p) => (p.style.translate = ''));
    });
  }

  // --- CTA form (demo only) ---
  document.querySelectorAll('form').forEach((form) => {
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const btn = form.querySelector('button');
      if (!btn) return;
      const original = btn.textContent;
      btn.textContent = '✓ You’re on the list!';
      btn.disabled = true;
      setTimeout(() => { btn.textContent = original; btn.disabled = false; form.reset(); }, 2600);
    });
  });
})();
