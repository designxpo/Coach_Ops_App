// CoachOps marketing site — interactions
import { initializeApp, getApps } from 'https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js';
import { getFirestore, collection, addDoc } from 'https://www.gstatic.com/firebasejs/10.12.0/firebase-firestore.js';

const firebaseConfig = {
  apiKey:            "AIzaSyDoL0oCduhmf3G6T10sdCCuU2NIV7g1_E8",
  authDomain:        "coachops-27a73.firebaseapp.com",
  projectId:         "coachops-27a73",
  storageBucket:     "coachops-27a73.firebasestorage.app",
  messagingSenderId: "566108244280",
  appId:             "1:566108244280:web:f9c48c0c548522846899cd",
};
const app = getApps().length ? getApps()[0] : initializeApp(firebaseConfig);
const db  = getFirestore(app);

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

  // --- Early access form ---
  const eaForm   = document.getElementById('early-access-form');
  const eaStatus = document.getElementById('ea-status');
  const eaSubmit = document.getElementById('ea-submit');

  if (eaForm) {
    eaForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      const name  = document.getElementById('ea-name').value.trim();
      const email = document.getElementById('ea-email').value.trim();
      const role  = eaForm.querySelector('input[name="ea-role"]:checked')?.value ?? '';

      if (!name || !email || !role) {
        showStatus('Please fill in all fields and pick a role.', 'error');
        return;
      }

      eaSubmit.disabled = true;
      eaSubmit.textContent = 'Sending…';

      try {
        await addDoc(collection(db, 'early_access'), {
          name,
          email: email.toLowerCase(),
          role,
          signedUpAt: Date.now(),
          source: 'website',
        });
        showStatus("You're on the list! We'll be in touch soon.", 'success');
        eaForm.reset();
        eaSubmit.textContent = '✓ Done';
      } catch (err) {
        console.error('early_access write failed', err);
        showStatus('Something went wrong — please try again.', 'error');
        eaSubmit.disabled = false;
        eaSubmit.textContent = 'Request early access';
      }
    });
  }

  function showStatus(msg, type) {
    eaStatus.textContent = msg;
    eaStatus.hidden = false;
    eaStatus.className = 'ea-status ea-status--' + type;
  }
})();
