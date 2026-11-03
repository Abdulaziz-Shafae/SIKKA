# Sikka — final review on testing

**Working branches only:** SIKKA/testing and Sikka-front/testing.

**Immutable approved baselines:**
- Backend: `deployment/brevo-railway` (including its Brevo notification behavior)
- Frontend: `fix/team-feedback-20261027` (including its current ride / meal / restaurant UX)

No implementation from older, deleted review branches is imported automatically. Each change needs approval before implementation.

## Counter — 2 / 14

- [x] 01 — **Temporary unlimited free visits / Moyasar disabled:** `SIKKA_PAYMENTS_ENABLED=false` default; FREE_UNLIMITED entitlement and zero price; no checkout; all visit kinds, reused visits and re-generation bypass payment checks. Existing paid logic, Moyasar integration and historical payment data remain. Inline comments in affected code describe how to reactivate. New tests assert zero price, unlimited eligibility and payment endpoint guards. Back-end CI: 129/129 passing; frontend typecheck, flow/integration checks and Railway build: passing.
- [x] 02 — **Email-only account lifecycle:** new registrations require verification before login, email-only account verification and password reset; paused phone collection/verification/WhatsApp recovery kept in comments. Profile onboarding is preserved after verifying and logging in. Authenticated users can request irreversible account deletion with password confirmation; solo visits are removed, shared visits retain other members with anonymized deleted-account placeholders, and other active sessions are invalidated. Backend H2 regression tests cover registration, email-only verification/recovery, solo/guest/organizer deletion, invalid password and session invalidation. Tested on CI, live MySQL/Railway remains point 14.
- [ ] 03 — Visit creation, group members, invitations and RSVP
- [ ] 04 — Ride selection and priorities
- [ ] 05 — Current approved meals/snacks/restaurant UX (do not change numbers without consent)
- [ ] 06 — Initial planning, approvals and regeneration (do not alter AI usage or arrival AI behavior without approval)
- [ ] 07 — Live visit activity workflow and replan
- [ ] 08 — Prayer scheduling
- [ ] 09 — Map and navigation
- [ ] 10 — Brevo and n8n notifications (Brevo implementation must remain unchanged until separately approved)
- [ ] 11 — Visit rating, recap and image
- [ ] 12 — Frontend simplification and responsive / accessibility
- [ ] 13 — Security, authorization and privacy
- [ ] 14 — End-to-end on Railway Testing, MySQL validation, required data comparison, release readiness

**Verification for point 01:** Static review plus CI success, not yet validated on the live Railway Testing environment. No production branch changes, merges, or deploys.

**Reactivation:** On the backend, set `SIKKA_PAYMENTS_ENABLED=true` after separately testing Moyasar, legacy two-free-visits quota, persisted payment orders, checkout and webhooks. The frontend checks the backend entitlement state, so it can show the existing checkout when payment is required.
