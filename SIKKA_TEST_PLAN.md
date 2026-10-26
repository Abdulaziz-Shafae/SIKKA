# SIKKA — Deferred Integration Test Plan

Status: TESTS DEFERRED by project owner. This file is a checklist, not proof of successful execution.
Branch: feature/four-updates
Do not merge into main until every critical case is verified.

## Environment and preparation
1. Configure MySQL, Spring Boot environment, OpenAI credentials, and sample park data.
2. Use test accounts: organizer, adult group member, family member with minor, and unrelated account.
3. Create SOLO, FRIENDS (together and split), and FAMILY visits.
4. Record the visit IDs, member IDs, generated plan IDs, and test ride IDs.
5. Confirm park statuses, directed walking routes, planning wait estimates, eligibility, meals, and prayer data.
6. Run `./mvnw clean test` (Windows: `.\mvnw.cmd clean test`) after implementation is complete.
7. Check DB migrations / schema updates for VisitPlan.excludedRides.

## Task 1 — MUST_RIDE
- [ ] A feasible MUST_RIDE is scheduled once for its selected participant.
- [ ] If all selected MUST_RIDE do not fit, return the best feasible subset for MAX_RIDES.
- [ ] MIN_WALKING / walking reduction goal prefers the lower walking feasible option.
- [ ] MORE_REST goal preserves chosen rest intervals rather than adding distant rides.
- [ ] Missing or untrusted waiting estimates do not become zero-minute waits.
- [ ] Missing directed route is not replaced with fictional straight-line walking.
- [ ] CLOSED ride is excluded with grounded reason; do not incorrectly reject entire plan.
- [ ] NOT_ELIGIBLE participant cannot be scheduled. UNKNOWN is provisional only with warnings.
- [ ] AVOID, disliked ride features and minor supervision remain enforced.
- [ ] Meals have 60 minutes, snacks 30 minutes, initial prayer reserve 15 minutes.
- [ ] No overlapping participant activities and no visit-deadline overflow.
- [ ] Every omitted MUST_RIDE has exactly one reason record per participant.
- [ ] No participant's ride is simultaneously scheduled and excluded.
- [ ] GET existing plan-details endpoint returns excluded rides for GROUP and correct filtered INDIVIDUAL view.
- [ ] Existing original selections persist after generation and regeneration.
- [ ] Infeasible empty plan is not persisted as a valid plan.
- [ ] Free entitlement is per VISIT, not per generate/re-generate request. Verify billing separately before launch.

## Task 2 — Replanning
- [ ] Member can request an INDIVIDUAL proposal for their own solo editable items.
- [ ] Member cannot edit another member's activities or shared group itinerary.
- [ ] Organizer can request a GROUP proposal.
- [ ] Closed MUST_RIDE can be replaced when authorized without failure during snapshot preparation.
- [ ] Earlier completion is detected from recorded execution release time, not assumed.
- [ ] Previously excluded MUST_RIDE is reconsidered when time genuinely becomes available.
- [ ] Finished, ongoing, pinned, meal, prayer and meeting point items are preserved.
- [ ] Alter no more than 25% of remaining substantive activities unless approved one-item exception.
- [ ] Proposal stays pending until the proper approver responds.
- [ ] Rejection leaves approved itinerary unchanged.
- [ ] If no real improvement is feasible, keep the existing itinerary without unnecessary alerts.

## Task 3 — Map API
- [ ] GET /api/v1/map/get/data returns park image and mapped ride/place coordinates.
- [ ] GET /api/v1/map/get/walking-context returns the same directed network used during planning.
- [ ] GET /api/v1/map/route/{fromType}/{fromId}/{toType}/{toId} returns a navigable path, not an arbitrary straight line.
- [ ] Missing routing data returns an explicit unavailable/error state.
- [ ] Plan-details PlanItems have usable rideId/placeId, chronology, startsAt/endsAt and participant identity.
- [ ] Frontend can focus map marker from its matching schedule card.
- [ ] For closed/missing place, do not invent a marker or entrance coordinate.
- [ ] Guest public map endpoints remain accessible, protected edit endpoints remain protected.
- [ ] Verify the Nawaf image and node mapping physically match park locations.

## Task 4 — Google Calendar
- [ ] OAuth connect and status work with correct account.
- [ ] Syncing approved plan creates one event.
- [ ] Syncing the same visit again updates the existing Google event (no duplicate).
- [ ] Updating visit time/date and explicitly re-syncing changes the existing event.
- [ ] Cancelled visit is denied new sync.
- [ ] DELETE /api/v1/google-calendar/sync/cancelled/{visitId} removes an existing cancelled event, or returns success when already absent.
- [ ] Unauthorized user cannot remove a different person's event.
- [ ] Token expiry is refreshed; revoked refresh tokens produce actionable error.
- [ ] Google network failure never cancels or rolls back the core visit plan.
- [ ] Disconnect deletes local stored authorization and event mappings.
- [ ] Check if remote events persist after disconnect and communicate policy clearly.
- [ ] Automatic sync after plan approval/change is NOT assumed until actually wired and confirmed.

## Acceptance record
| Area | Tester | Date | Outcome | Issues |
| --- | --- | --- | --- | --- |
| MUST_RIDE | | | NOT RUN | |
| Replanning | | | NOT RUN | |
| Map API | | | NOT RUN | |
| Google Calendar | | | NOT RUN | |
| Security / approvals | | | NOT RUN | |
| Entitlements / billing | | | NOT RUN | |

## Release gate
Do not merge to main until compile, automated tests, manual API flows, park map coordinates, and visit-based billing are verified.

## Implementation audit notes (before running tests)
- Initial plan generation still relies on AI candidate selection; the backend validates omissions but does not yet independently compute or prove the best feasible subset. Do not mark Task 1 complete.
- Replanning currently preserves the original activity count and order; inserting a newly feasible excluded MUST_RIDE is not implemented. Do not mark Task 2 complete.
- Existing MapController exposes GET /get/data, /get/walking-context and /route/{fromType}/{fromId}/{toType}/{toId}. End-to-end mapping of planned item order to individual walking route segments must be verified before marking Task 3 complete.
- GoogleCalendarOAuthService supports explicit sync upsert, reconnect recovery for deleted remote events, disconnect, and cancelled-event removal. Automatic background sync triggered by plan changes is not implemented. Do not mark Task 4 complete.
- A successful compilation and integration test have not been observed for the branch. Testing is intentionally deferred.

## Current implementation limits — do not treat these as passed tests
- The server-side MustRideSubsetService packs conservative candidates from known wait times, directed walking routes and reserved breaks. This is a *hint*, not an exact optimizer or proof that the selected subset is optimal.
- Existing REPLAN still requires unchanged activity count and order and cannot insert a newly feasible excluded MUST_RIDE into an itinerary; this is an outstanding implementation requirement.
- Map route endpoint returns consecutive same-participant segments and declares that the entrance segment is not included. A true per-member split itinerary map is still required for complete multi-group routing.
- The Calendar after-commit event handles organizer sync on approval/arrival and organizer event cancellation. Other linked members' events are not automatically synchronized.
- Exact permitted primaryGoal values for less walking and more rest were not found in the backend's validation contract. Do not introduce guessed enum names.

## New branch changes to verify
- [ ] GET /api/v1/visit-plan/get/map-route/{visitPlanId} returns authorized planned activities and a segment for each consecutive routable activity with the same participant IDs. Split-group transitions do not become fictional paths; entrance route is explicitly not supplied.
- [ ] After APPROVED replanning, a connected organizer's existing Google event is updated only after database commit.
- [ ] Arrival scheduling updates a connected organizer's event after successful schedule commit.
- [ ] Cancelling a visit schedules organizer Google event removal after commit; failure does not undo local cancellation.
- [ ] Verify no Google event is created when not connected.
- [ ] Verify 15-minute prayer activity duration on both initial reserve and actual ARRIVAL prayer event checks.
