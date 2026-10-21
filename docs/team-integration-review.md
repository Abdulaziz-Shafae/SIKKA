# Team integration review — 21 October 2026

## Scope and authoritative references

Merged the current `mohammed` and `nawaf` heads, retaining `shefo` (already an ancestor of main), and incorporated `codex/park-catalogue-fields`. Reviewed the supplied coding instructions and project overview, the original flow-linking contract, the newer Sikka_Final_Team_Execution_Agreement.docx, Sikka_Facilities_SQL_JSON.docx and Sikka_Facilities_Field_Comparison.xlsx. The newer agreement takes priority over the original contract.

## Corrections

- RegisterDtoIn, VisitorProfileDtoIn and VisitorProfile accept LOW/MILD/EXTREME; the subsequent service validation review restricts these to LOW/MILD/EXTREME only.
- Place filtering accepts SHOP as well as the existing place kinds. Preserved dining service, cuisine, retail types, land, companion conditions and recommended ages from the catalogue branch.
- Visit states: DRAFT, PLANNING, READY, IN_PROGRESS, COMPLETED, CANCELLED. Removed legacy OPEN checks from plan services; OPEN remains valid for Ride/Place operating status.
- Payment statuses: PENDING/PAID/FAILED. Consent kinds: HEALTH_STORAGE/LOCATION_SHARING/WHATSAPP/EMAIL_MARKETING. VisitRecord: COMPLETED/SKIPPED. Notification: PENDING/SENT/FAILED; added readAt without introducing READ.
- Added missing hasCompletedRide used by RidePreferenceService (the merged source previously could not compile), sendNotification delegating to existing notification creation, and getPlanStatus rejecting empty/untimed group plans.
- Plan creation requires PLANNING, verified payment and the organizer; fixes the roster lock on the existing creation paths. Legacy member deletion now checks the lock and protects the organizer.
- Approval uses VisitService.changeStatus instead of assigning READY directly.
- getOpenRides returns only open rides with a nonfuture wait observation, nonempty source and a reading within configurable `sikka.park.wait-freshness-minutes` (default 15 minutes). This is an internal freshness threshold, not a claim from Six Flags.
- Updated catalogue tests for the merged RideService constructor.

## Validation

`python scripts/check_park_patterns.py` passes for all 83 supplied JSON records: 32 rides, 27 restaurants and 24 shops; 338 constrained values checked. The fixture is extracted from the supplied document, not refreshed website data. This checks vocabulary compatibility, not SQL execution, JSON deserialization or live operating status.

`git diff --check` passes. Added Java regression tests for official thrill labels, shop filtering, obsolete states, fresh wait selection and completed ride lookup.

`bash mvnw test -q` is BLOCKED before compilation: the environment cannot resolve repo.maven.apache.org to download the existing Spring Boot 4.1.1 parent. Neither Java compilation nor JUnit/database execution is claimed to pass. No framework version was changed to work around the network failure.

## Agreement items still requiring implementation

This merge aligns existing CRUD and validation; it does not complete all planned features.

- VisitPlanService.startPlanning and onRideChanged are still absent. No AI generation/retry/group-regeneration implementation, durable failure workflow or replan proposal generation/application exists.
- PaymentOrderService creates pending payment attempts; actual Moyasar server verification and the resulting PLANNING transition are not implemented. No browser-supplied payment success is trusted by these changes.
- Notification sending currently queues PENDING records; delivery workers/providers and readAt endpoint are not implemented.
- Eligibility deliberately returns UNKNOWN unless a known height/weight/age rule is violated. Companion/health/visit-date-aware checks remain incomplete; UNKNOWN must never be advertised as safe.
- Selected-rides JSON validation, consent enforcement before health storage and notification delivery, concurrent roster mutation, approval/payment races, and full end-to-end tests remain outstanding.
- Existing authentication/verification/reset features and the accountId path convention remain development work. Path IDs are not authentication.

Catalogue records stay UNAVAILABLE with missing restrictions/coordinates/waits null; this merge does not insert production data, resolve the conflicting Adrena-Line heights, or connect queue-times.
