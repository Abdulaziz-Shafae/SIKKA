# Sikka — final review on testing

**Working branches only:** SIKKA/testing and Sikka-front/testing.

**Immutable approved baselines:**
- Backend: `deployment/brevo-railway` (including its Brevo notification behavior)
- Frontend: `fix/team-feedback-20261027` (including its current ride / meal / restaurant UX)

No implementation from older, deleted review branches is imported automatically. Each change needs approval before implementation.

## Counter — 1 / 14

- [x] 01 — **Temporary unlimited free visits / Moyasar disabled:** `SIKKA_PAYMENTS_ENABLED=false` default; FREE_UNLIMITED entitlement and zero price; no checkout; all visit kinds, reused visits and re-generation bypass payment checks. Existing paid logic, Moyasar integration and historical payment data remain. Inline comments in affected code describe how to reactivate. New tests assert zero price, unlimited eligibility and payment endpoint guards. Back-end CI: 129/129 passing; frontend typecheck, flow/integration checks and Railway build: passing.
- [ ] 02 — Account creation, email verification, login, account deletion and personal data
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
