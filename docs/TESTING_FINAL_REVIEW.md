# Sikka — final review on testing

**Working branches only:** SIKKA/testing and Sikka-front/testing.

**Immutable approved baselines:**
- Backend: `deployment/brevo-railway` (including its Brevo notification behavior)
- Frontend: `fix/team-feedback-20261027` (including its current ride / meal / restaurant UX)

No implementation from older, deleted review branches is imported automatically. Each change needs approval before implementation.

## Counter — 2 / 14

- [x] 01 — **Temporary unlimited free visits / Moyasar disabled:** `SIKKA_PAYMENTS_ENABLED=false` default; FREE_UNLIMITED entitlement and zero price; no checkout; all visit kinds, reused visits and re-generation bypass payment checks. Existing paid logic, Moyasar integration and historical payment data remain. Inline comments in affected code describe how to reactivate. New tests assert zero price, unlimited eligibility and payment endpoint guards. Back-end CI: 129/129 passing; frontend typecheck, flow/integration checks and Railway build: passing.
- [x] 02 — **Email-only account lifecycle:** new registrations require verification before login, email-only account verification and password reset; paused phone collection/verification/WhatsApp recovery kept in comments. Profile onboarding is preserved after verifying and logging in. Authenticated users can request irreversible account deletion with password confirmation; solo visits are removed, shared visits retain other members with anonymized deleted-account placeholders, and other active sessions are invalidated. Backend H2 regression tests cover registration, email-only verification/recovery, solo/guest/organizer deletion, invalid password and session invalidation. Tested on CI, live MySQL/Railway remains point 14.
- [ ] 03 — Visit creation, group members, invitations and RSVP: implemented targeted hardening on testing (active + verified join gate, DRAFT-only invitation rotation/toggle, duplicate/capacity rules preserved). Added integration regression tests; frontend now exposes MAYBE and restricts member/family controls and stale invitation sharing to DRAFT. **Pending CI result and live Railway/MySQL validation** before marking complete.
- [ ] 04 — Ride selection and priorities
- [ ] 05 — Current approved meals/snacks/restaurant UX (do not change numbers without consent)
- [ ] 06 — Initial planning, approvals and regeneration (do not alter AI usage or arrival AI behavior without approval)
- [ ] 07 — Live visit activity workflow and replan: frontend-only changes on `Sikka-front/testing` require explicit skip confirmation, expose manual `suggest-replan` with reason, and clarify proposal remains non-operative pending approval. Added `live-flow.cjs` contract/source assertions. **CI/build and Railway end-to-end not yet verified**.
- [ ] 08 — Prayer scheduling: frontend display changes on `Sikka-front/testing` show server-confirmed prayer name, calculate actual prayer start from scheduled walking start, show known prayer-room name, and label times provisional before confirmed arrival. Added `live-flow.cjs` assertions. CI/build and Railway/MySQL checks pending.
- [ ] 09 — Map and navigation
- [ ] 10 — Brevo and n8n notifications (Brevo implementation must remain unchanged until separately approved)
- [ ] 11 — Visit rating, recap and image
- [ ] 12 — Frontend simplification and responsive / accessibility
- [ ] 13 — Security, authorization and privacy
- [ ] 14 — End-to-end on Railway Testing, MySQL validation, required data comparison, release readiness

**Verification for point 01:** Static review plus CI success, not yet validated on the live Railway Testing environment. No production branch changes, merges, or deploys.

**Reactivation:** On the backend, set `SIKKA_PAYMENTS_ENABLED=true` after separately testing Moyasar, legacy two-free-visits quota, persisted payment orders, checkout and webhooks. The frontend checks the backend entitlement state, so it can show the existing checkout when payment is required.

## Point 03 audit notes (2026-11-03)

- Inspected `VisitController`, `VisitMemberController`, `VisitGroupController`, corresponding services, creation DTOs, front-end `live-api`, `backend-contract`, `live-sikka` and `group-invitation`.
- Existing creation, group invitation, RSVP, leave/removal, family capacity confirmation and visit lifecycle APIs were retained. Admin-only legacy CRUD routes are separately restricted by `SecurityConfig` and left unchanged.
- Previous joining code checked account existence and visitor profile but not account ACTIVE or email verified; now both are required. Existing duplicate/capacity checks remain in `createMember`.
- Previous invitation rotate/toggle methods did not enforce DRAFT; they now lock the visit row and reject locked/non-DRAFT changes. Existing code and toggling behavior remain in place.
- Previous frontend omitted MAYBE RSVP and displayed removal/family controls when status was not DRAFT. Changed to match backend. Old locally stored invitation code no longer displayed or converted to a link after the visit is locked.
- No main, approved baseline, external notification provider, n8n, payment gateway, deployment, or Production changes. Not all end-to-end paths can be verified through repository inspection alone; CI and staging behavior must be checked separately.

## Point 07 approved changes (2026-11-03)

- User approved frontend skip confirmation and manual replan action, retaining existing AI, notification, and execution backend semantics.
- `components/sikka/live-plan.tsx`: skip button now opens an explicit confirmation dialog, and API call is only made through the dialog's confirmation; cancellation closes without server request.
- The manual replan form appears for active approved visits in progress and without an outstanding proposal. It requires a nonempty reason and calls the pre-existing `/api/v1/visit-plan/suggest-replan/{visitId}` endpoint; the proposal still requires approval and does not automatically replace the active schedule.
- Added explanatory message to the proposal area and basic integration/source assertions in `scripts/checks/live-flow.cjs`.
- No backend, AI, notification, paid-mode, main branch, production deployment, or approved baseline modifications in this point. Full runtime tests and staging validation remain pending.

## Point 08 approved frontend changes (2026-11-03)

- User approved improving prayer title/time/place display and clarifying provisional times before arrival confirmation, with no modification to AI, Aladhan, or notifications.
- `lib/prayer-presentation.ts`: derives display values strictly from plan item `title`, `startsAt`, `walkingMinutes`, and known PRAYER_ROOM place; never guesses the prayer name from clock time. For scheduled prayer, item `startsAt` is walking start and prayer time is walking start plus walking minutes.
- `components/sikka/live-plan.tsx`: shows confirmed prayer name/time/prayer-room when provided; otherwise clearly distinguishes unknown or provisional values. Non-prayer activity display unchanged.
- `scripts/checks/live-flow.cjs`: new presentation and source-wire assertions. Actual Node check, TypeScript build, and Railway end-to-end testing remain pending.
- Backend runtime unchanged, including prayer API, scheduling, AI, approved baseline, Brevo, n8n and Production.
