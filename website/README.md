# ProCoach India — Marketing Website

A single-page marketing site for **ProCoach India** — a fitness platform that is
*two apps in one*: a **business cockpit for coaches** and a **personal fitness
center for members**. Built in the app's dark + neon-lime (`#B9F501`) theme with a
complete **liquid-glass** (glassmorphism) design system and the official
ProCoach India logo. Every phone mockup uses the **real app UI**.

## Files
```
website/
├── index.html      # all page markup
├── styles.css      # theme tokens + liquid-glass system + responsive layout
├── script.js       # nav scroll state, mobile menu, scroll-reveal, hero parallax
└── assets/
    ├── logo-mark.svg / logo-full.svg     # ProCoach India logo (transparent)
    ├── app-home.jpg      coach: home dashboard
    ├── app-members.jpg   coach: roster
    ├── app-programs.jpg  coach: program tracker
    ├── app-revenue.jpg   coach: revenue & billing
    ├── app-library.jpg   coach: program library
    ├── app-diet.jpg      member: Build Muscle Indian diet plan
    ├── app-bmi.jpg       member: BMI & body composition
    ├── app-fitness-home.jpg  member: steps/sleep/energy + body tracking
    ├── app-fitness.jpg   member: fitness/nutrition cards
    ├── app-discover.jpg  member: discover coaches
    └── app-bookings.jpg  member: bookings
```

## Run locally
```bash
cd website && python3 -m http.server 8753   # then visit http://localhost:8753
```

## Structure (balanced for both audiences)
Nav (logo) · Hero (Coach + Member phones) · Trust strip · **Audience split** ·
**For Coaches** (3-phone fan + features) · **Fitness Center / For Members** (3-phone
fan + diet/BMI/progress feature grid) · Stats · Pricing · Reviews (coach + member) ·
CTA · Footer.

## Design notes
- **Theme tokens** live in `:root` in `styles.css` — change `--lime` once to re-skin.
- **Liquid glass** is the `.glass` primitive: translucent gradient + `backdrop-filter`
  blur/saturate + 1px border + inset top highlight + soft drop shadow.
- **Logo**: `logo-full.svg`/`logo-mark.svg` were derived from the brand SVG with the
  background plate removed so they sit transparently on the dark UI.
- **Fonts**: Space Grotesk (display) + Inter (body), with a system-font fallback.
- **Responsive** to ~360px (verified `scrollWidth=390`); honours `prefers-reduced-motion`.

## Deploy (Vercel)
```bash
cd website && vercel deploy --prod --scope designxpos-projects
```
