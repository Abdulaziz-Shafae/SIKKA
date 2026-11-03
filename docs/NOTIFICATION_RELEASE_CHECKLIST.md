# SIKKA — Notification release checklist

Changes in `feature/notifications-approved-20261103` are intentionally **not deployed** until this checklist passes against the real staging backend and frontend.

## 1. Required backend configuration

Set in Railway **backend** Variables, not GitHub:
- `BREVO_API_KEY` — valid Brevo transactional mail API key
- `MAIL_FROM` — a verified sender address in Brevo
- `SIKKA_NOTIFICATION_CRON_KEY` — strong, random private string (do not share in frontend)
- `SIKKA_NOTIFICATIONS_REMINDERS_ENABLED=false` — disable frequent in-process scanning for serverless
- `SIKKA_WAIT_CHANGE_MINUTES=15`, `SIKKA_DELAY_GRACE_MINUTES=20`

A unique `EMAIL` notification row per recipient/event prevents duplicate queuing. Failure status is recorded in the notification table. Never commit production secrets.

## 2. External reminder cron, without keeping the service awake

Configure an **n8n Schedule Trigger every 5 minutes** and HTTP Request node:
- Method: `POST`
- URL: `https://<backend-deployment-domain>/api/v1/internal/notification-cron/run`
- Header: `X-SIKKA-CRON-KEY: <same SIKKA_NOTIFICATION_CRON_KEY>`
- No request body is needed.
- Expected success JSON: `{"scannedVisits": <integer>}`; a wrong or absent key yields 404.
- Use an internal protected n8n credential for the header, not a hardcoded text node exported with the workflow.

The backend permits late scans within a bounded window: 24h reminder up to 1h after nominal time, 2h reminder until visit start. Delivery is deduplicated using the visit start datetime so rescheduling gets new keys. If Railway is sleeping, this external POST wakes it; do not enable internal scanning simultaneously without a reason.

## 3. Database safety

1. Back up MySQL before deployment.
2. Confirm adding `visit.completed_at` and `plan_item.ride_alert_deferred` and Hibernate's DDL update succeed against a **copy of the real schema**.
3. Startup migration converts historical `visit_member.rsvp_status='MAYBE'` to `INVITED` (unanswered), never to `GOING` or `NOT_GOING`.
4. Verify old pending plans, approvals and activity records still load.

## 4. Functional staging tests

- FRIENDS together: all invitees answered `GOING`/`NOT_GOING` -> one organizer readiness notification; no ride answers needed.
- FRIENDS split: wait for `ANSWERED` or `SKIPPED` for every `GOING` attendee, excluding `NOT_GOING`.
- FAMILY and SOLO: no 'all ready' notification.
- Manual DRAFT reminder: in-app + email; no WhatsApp.
- APPROVED plan: attendees only; in-app toast + email; organizer excluded.
- Replan: organizer sees old/new; reject keeps schedule; approve notifies affected members only.
- Closed ride: organizer action DEFER reorders remaining rides with verified walk routes; alert returns near the deferred game's new time if still closed. REPLACE must choose an open ride with verified eligibility; fail visibly if no safe candidate.
- +15m wait: one light in-app notice; +30m: keep all / replace / remove lower-priority ride. Confirm no pinned prayer/meeting/started activity is changed; verify revised walk route and duration.
- Delay +20m: one response with reason, without automatic replan.
- Restaurant: after penultimate completed ride, only if not selected; organizer picks cuisine, server validates restaurant route/time before applying.
- Payment success/failure: owner only, verified Moyasar response, email only.
- Reminder: correct recipient, once at 24h and 2h; test cancelled/rescheduled visit and missed 5-minute cron.
- Complete visit and both ratings: immediate authorized recap page and exactly one Brevo email with actual played rides, elapsed time and attached zigzag PNG. No in-app completion notification.
- Repeat account logins, reload, transient network errors, accessibility and mobile layout.

## 5. CI and deployment gate

Run backend Maven tests and frontend typecheck, UI flow, integration contract and production build. Passing CI does **not** prove Brevo, Moyasar, n8n, production schema, Railway sleeping/waking, GPS/maps or browser flows. Mark deployment **NO-GO** until the staging smoke checks and actual external mail delivery complete. Review both draft PRs, merge backend into `deployment/brevo-railway` and frontend into `main` only when authorized, and verify production deployment health and logs.
