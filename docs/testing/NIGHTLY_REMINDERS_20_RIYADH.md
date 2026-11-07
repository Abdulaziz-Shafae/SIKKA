# Night-before READY-plan reminders — 20:00 Asia/Riyadh (testing)

## Status
Code prepared in backend `testing` only, feature OFF by default.
No Railway deployment, no scheduled job, no external emails were created or sent.

## Schedule
- Use an EXTERNAL scheduler (not an application `@Scheduled` loop) to send one HTTPS POST each night at **20:00 Asia/Riyadh**, equivalent to **17:00 UTC**.
- Standard five-field UTC cron: `0 17 * * *`.
- URL: `POST /api/v1/nightly-reminder/run` on the securely reachable backend. The backend currently has no public Railway domain; an authenticated scheduler/private connectivity or a carefully restricted public endpoint would need setup.
- Request header: `X-Sikka-Nightly-Key: <dedicated-random-32+-character-secret>`. NEVER commit the secret.

## Only for later authorized activation
- Set `SIKKA_NOTIFICATIONS_NIGHT_BEFORE_ENABLED=true` (Spring property `sikka.notifications.night-before-enabled` must map to the environment; code currently reads that property).
- Set `SIKKA_NIGHTLY_REMINDER_KEY` to a cryptographically random value at least 32 characters.
- External scheduler stores the same key as a secret and triggers the POST once daily. Do not configure this until isolated tests pass and real email sending is explicitly approved.
- Keep `SIKKA_NOTIFICATIONS_REMINDERS_ENABLED=false` for the legacy every-minute scheduler unless it is needed separately.

## Eligibility and delivery
- Tomorrow's `READY` visits with an active GROUP plan whose approval status is `APPROVED`.
- Before each send, the service reloads the visit under a DB lock and rechecks date, READY, active and approved.
- Each `GOING` member with an account gets one email; linked family members without an account use the organizer's email. NotificationEventService already groups recipients by account.
- Existing NotificationEventService.save and queueEmail provide the `NIGHT_BEFORE_VISIT_REMINDER:<visitId>:<date>` event key per account and queue via existing n8n notification channel. Duplicate calls for the same day avoid resending in normal sequential execution.
- The endpoint checks a 20:00–20:29 Riyadh window, and returns an error outside it. A sleeping Railway service is expected to wake on the external trigger and may incur a cold start.
- Scheduler should log outcome; if email delivery fails, existing email notification status tracks FAILED. Failed delivery retries are not automatically guaranteed.
- This nightly job **does not keep the backend awake**, but existing 60s ActivityDelayMonitor and 120s RideChangeMonitor may still prevent sleeping. Do not promise sleep until they are made idle-aware and verified.

## Testing before enabling
1. Run backend `./mvnw verify`; check unit test `NightBeforeVisitReminderServiceTest` and normal security/notification tests.
2. Verify disabled endpoint returns 404 and causes no database reads or email deliveries.
3. Test 403 for wrong/missing secret, 409 outside 20:00–20:29, and no sends for draft/planning/completed/cancelled or unapproved plans.
4. Use mocked email transport or n8n test endpoint; test GOING/organizer/family recipient mapping and duplicate replays.
5. Deploy only to Railway Staging with explicit approval; leave flag OFF until tests are complete.
6. Once enabled, test 17:00 UTC scheduler trigger, logs, cold start and Railway sleep. No DB migration needed.

## Rollback
Remove/disable the external scheduler first; set `SIKKA_NOTIFICATIONS_NIGHT_BEFORE_ENABLED=false`; redeploy Staging if required. Data remains unchanged.

## 2026-11-07 Railway Staging preparation (not deployed)

- The separate Bun Function `sikka-nightly-reminders-staging` is **STAGED** with cron `0 17 * * *`, with `SIKKA_NIGHTLY_CRON_ENABLED=false` and no sender secret. It is not a running Railway service and cannot send emails.
- SIKKA Backend and Frontend service source changes to branch `testing` are **STAGED**, not applied. Staged frontend env: `SIKKA_BACKEND_URL=http://sikka-backend-test.railway.internal:8080`, `NEXT_PUBLIC_SIKKA_API_URL=/api/backend`. Verify private host and port before applying. Existing legacy env values have not been deleted.
- Staged backend env: Hikari maximum pool 3, minimum idle 0, idle timeout 30000 ms, keepalive 0, plus `SIKKA_NOTIFICATIONS_NIGHT_BEFORE_ENABLED=false`.
- `VisitMonitorGate` checks for an IN_PROGRESS visit once at backend startup or after a successful start/complete HTTP request, and arms a 10-minute window on successful visit-plan / park-exploration requests. The existing monitors skip SQL/Queue Analytics network work outside that window, unless any visit remains IN_PROGRESS. This preserves in-progress monitoring even if a device is locked; no database migration.
- Scheduled jobs still wake in JVM time but return without database or HTTP access when idle. Whether Railway actually sleeps must be measured from runtime metrics/network flows after a successful test deployment.
- Added unit tests for monitor idle short-circuits, gate behavior, cron time window and unauthorized trigger. The unit test files have been committed but no verified Maven build/test results were accessible during this preparation. Do not accept the staged Railway patch until the build and full regression tests pass.
- To activate later: test n8n delivery in a non-production sink, securely stage identical >=32-character keys in backend and Function, set backend `SIKKA_NOTIFICATIONS_NIGHT_BEFORE_ENABLED=true` and Function `SIKKA_NIGHTLY_CRON_ENABLED=true`, then apply only after authorization. NEVER send test reminders to actual visit participants.
