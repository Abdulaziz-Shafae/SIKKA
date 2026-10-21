# Service validation review — approved for main

The user approved publishing these changes to main on 21 October 2026. The reviewed base is `cda32021f7b8525564318ab2ecb34a5a314203de`; the integration commit also records the reviewed mohammed, nawaf and shefo branch heads as parents. This review covers all 15 current service classes and all 34 @Pattern constraints. References are the final team execution agreement and facilities field comparison already reviewed in this session.

## Prepared changes

| File/class | Change |
| --- | --- |
| Ride, VisitorProfile, RegisterDtoIn, VisitorProfileDtoIn | Only LOW/MILD/EXTREME; reject MEDIUM/HIGH, lowercase and whitespace variants. Null remains allowed for these optional fields, preserving unknown data. |
| AccountService.register, VisitorProfileService.fill, RideService.validateRide | Also check the same three values for internal service calls before saving. |
| VisitService.add | Require active organizer with a profile; create the organizer's GOING membership in the same transaction, matching the newer create route. |
| VisitService.update | Take a visit write lock; reject updates after members are locked or outside DRAFT/PLANNING. |
| VisitService.add/create/update | Reject missing/past visit dates and nonpositive/missing participant capacity. Reject missing DTOs and null status changes with ApiException. |
| VisitMemberService | Reject null RSVP, rating outside 1–5, and missing invitation enablement without null dereferencing. |
| VisitGroupService | Reject empty invite codes, empty/oversized group names, duplicate group attachment and attachment after roster lock/visit closure; transactional group attachment with a visit lock on the account-facing add path. |
| RideService | Require a wait source when a wait value is supplied; reject missing ride bodies. |
| PlaceService | Reject missing place bodies and invalid place kinds before persistence. |
| PrivacyConsentService | Reject missing/invalid consent kind and missing/oversized policy version before persistence. |
| RidePreferenceService | Reject missing/invalid preference on add and update; preserve existing completed-ride and ownership checks. |
| PaymentOrderService | Reject an inactive buyer and missing visit date before creating an attempt. |
| PlanItemService | Reject missing/nonpositive plan item IDs before repository access. |
| Documentation | Remove the old MEDIUM/HIGH compatibility allowance. |

## Pattern audit

All 34 existing annotation regexes are anchored and accept their declared values. Repeated fields are consistent across model and DTO. Service subsets are intentional: RSVP mutation accepts GOING/MAYBE/NOT_GOING (INVITED is the initial internal state); approval responses accept APPROVED/REJECTED (PENDING is initial).

| Domain | Result |
| --- | --- |
| Visit | DRAFT/PLANNING/READY/IN_PROGRESS/COMPLETED/CANCELLED; OPEN not allowed. |
| Payment | INITIAL/RETRY; PENDING/PAID/FAILED only. |
| VisitRecord | COMPLETED/SKIPPED only. |
| Notification | IN_APP/EMAIL/WHATSAPP; PENDING/SENT/FAILED; READ not introduced. |
| Consent | HEALTH_STORAGE/LOCATION_SHARING/WHATSAPP/EMAIL_MARKETING. |
| Plans/activities | INDIVIDUAL/GROUP; RIDE/PLACE/BREAK/MEETING_POINT; MUST_RIDE/PREFERRED/OPTIONAL. |
| Ride preference | REPEAT/AVOID/NEUTRAL. |
| Park catalogue | Categories, lands, food service/cuisine and shop types accept all supplied catalogue values. |
| Account | USER/ADMIN and ACTIVE/SUSPENDED/BLOCKED; these are application rules, not park classifications. |

RideFeatures vocabulary and additional application place kinds are not exhaustively established by the two reference files. Existing application values were not deleted without a contract specifying their removal. Recommended ages remain distinct from mandatory minimum/maximum ages. The conflicting Adrena-Line height remains unknown.

## Review of all 15 services: remaining work

| Service | Reviewed existing checks / remaining issue |
| --- | --- |
| AccountService | Duplicate email and output DTO checks exist. Password input is currently assigned directly to passwordHash; hashing, authenticated ownership, verification and reset flow remain unimplemented. Email verification is not reset when an existing email changes. |
| VisitorProfileService | Profile existence and duplicate ownership checked. JSON fields are not structurally validated; healthData storage is not gated by HEALTH_STORAGE. Profile deletion can cascade to memberships and needs historical-data policy. |
| VisitService | State transition graph and organizer flow checked. Legacy update/delete routes still lack an acting identity; paid/history-bearing hard deletion is not deliberately guarded. |
| VisitMemberService | Membership, RSVP, capacity, same-visit removal and roster-lock checks reviewed. selectedRides JSON and ride IDs still need validation. Concurrent membership changes are not serialized with plan generation; check-in/review updates can race. |
| VisitGroupService | Organizer/invite checks reviewed. Legacy CRUD lacks acting identity; invite changes and group removal still need lifecycle/concurrency policy. |
| RideService | Cross-field ranges, coordinate pairs and wait timestamp/source checks reviewed. Full eligibility, companion/health conditions and visit-date-aware age remain unfinished; UNKNOWN is not safe eligibility. Admin identity enforcement and onRideChanged callback remain absent. |
| PlaceService | Dining/shop field separation and coordinate pairs checked. Admin authorization remains absent. |
| RidePreferenceService | Ownership, existing preference and completed-ride checks reviewed. Concurrent duplicate creation still relies on database uniqueness; active-account enforcement is inconsistent outside the payment/planning paths. |
| PaymentOrderService | Organizer, DRAFT-only payment, existing PAID/PENDING and retry sequencing checked under visit lock. Actual Moyasar verification/callback flow remains absent. |
| PrivacyConsentService | Latest consent/revocation checks reviewed. Concurrent grant/revoke and enforcement at health storage/delivery remain outstanding. |
| VisitPlanService | Organizer/payment/state/duplicate-plan checks reviewed. Existing individual CRUD only creates the acting organizer's plan, so multi-member generation needs the pending startPlanning implementation. getPlanStatus checks only nonempty/timed GROUP items, not complete JSON membership, required individual plans or all safety rules. AI/retry/replanning remain absent. |
| PlanItemService | Membership, owner/organizer, GOING, priority and inactive/unsubmitted-state checks reviewed. The service updates preferences only; it does not implement full generated plan validation. |
| PlanApprovalService | Organizer, GROUP, PLANNING, pending approval, nonempty activities and open rides checked. Does not validate eligibility, participant JSON, conflicting/overlapping activity schedules or all individual-plan completeness. Visit cancellation/approval races remain unverified. |
| VisitRecordService | Same-visit GOING participants, distinct IDs, status, correction and self-rating checked. Progress aggregation is not implemented; concurrent terminal-state transitions remain unverified. |
| NotificationService | Active recipient, channel, content lengths and same-visit membership checked. Queues PENDING only; WhatsApp consent, providers/workers and readAt API still absent. |

## Validation evidence and limits

- PASS: 34 @Pattern regexes, 300 positive/negative cases run using the installed Java regex engine.
- PASS: 83 supplied catalogue records, 338 constrained values via `python scripts/check_park_patterns.py`.
- PASS: `git diff --check`.
- Added seven service regression tests and extended existing bean-validation tests to reject legacy thrill levels. These tests have NOT run.
- BLOCKED: Maven/JUnit/build; the existing Spring Boot 4.1.1 parent is not cached and Maven Central cannot be resolved from this environment. No dependency versions were altered.
- No runtime, database, concurrency, API authentication or end-to-end success is claimed by the static checks above.
