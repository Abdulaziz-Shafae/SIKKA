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
