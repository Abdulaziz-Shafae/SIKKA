<div align="center">

<img src="sikka-logo.png" alt="SIKKA logo" width="200" />

</div>

# SIKKA | سكة — API Documentation

> **API reference · 28 October 2026**  
> Backend repository: https://github.com/Abdulaziz-Shafae/SIKKA-final  
> Reference repository: `SIKKA-final`, branch `main`, commit `978bb68813`.

## Project links and queue data sources

| Resource | Link | Purpose |
|---|---|---|
| SIKKA website (public demo) | [Open SIKKA](https://sikka.up.railway.app/#home) | User-facing park planning experience; current live availability has not been rechecked here |
| Backend source (final) | [SIKKA-final](https://github.com/Abdulaziz-Shafae/SIKKA-final) | Source of truth for the 170 backend routes documented below |
| Frontend source | [SIKKA-front](https://github.com/Abdulaziz-Shafae/SIKKA-front) | Next.js/React application |
| Queue analytics and data collection source | [Queue_Analytics](https://github.com/Abdulaziz-Shafae/Queue_Analytics) | Independent queue data collection and analytics project; repository link, not a verified public dashboard URL |
| Queue-times source | [Queue-Times park 335 JSON](https://queue-times.com/parks/335/queue_times.json) | External queue/status feed used by the collection pipeline; freshness and availability vary |

**Deployment note:** No independently verified public URL is available here for the analytics dashboard or data collector. Add one only after confirming its actual deployment address; do not use a GitHub code URL as a substitute for a running site.

## API at a glance

| Item | Value |
|---|---:|
| Base path | `/api/v1` |
| REST endpoint mappings | **170** |
| Spring MVC controllers | **22** |
| JPA models | **22** |

**Counting rule:** one HTTP method + route pattern = one endpoint. Frontend Next.js routes, query variants, service methods, and scheduled jobs are excluded. This file documents declared backend routes; their availability in a running deployment still depends on configuration and tests.

## Authentication and access

- Spring Security supports authenticated application sessions. Browser requests must preserve the session cookie where required.
- Some API clients can use HTTP Basic credentials, subject to security configuration.
- Public park discovery and registration routes may be unauthenticated; admin catalogue maintenance requires appropriate authorization.
- Acting account identity is derived from the authenticated principal for user-scoped operations; path IDs generally identify resources rather than authenticate a caller.
- Do not put credentials, API keys or live user data in examples or repository files.

## Request and response conventions

- Most operations exchange JSON; put `Content-Type: application/json` on JSON mutation requests.
- Path placeholders such as `{visitId}` and `{planItemId}` must be replaced with IDs from actual responses.
- Body schemas and validation rules live in `src/main/java/com/example/sikka/DtoIn/`; response DTOs live in `DtoOut/`.
- Collection and response shapes vary across endpoints. No single wrapper or status is guaranteed for every handler.
- Typical validation/business errors return 400, unauthorized requests return 401, and forbidden access returns 403; check ControllerAdvice and SecurityConfig for precise behavior.
- `200` responses do not establish that external SMS/email/AI/calendar providers succeeded; provider errors and asynchronous actions require separate verification.

## API groups

| Controller path | Routes |
|---|---:|
| `/api/v1/account` | 6 |
| `/api/v1/auth` | 6 |
| `/api/v1/visitor-profile` | 6 |
| `/api/v1/visit` | 14 |
| `/api/v1/visit-group` | 9 |
| `/api/v1/visit-member` | 18 |
| `/api/v1/family-member` | 5 |
| `/api/v1/ride` | 12 |
| `/api/v1/ride-preference` | 5 |
| `/api/v1/place` | 6 |
| `/api/v1/map` | 8 |
| `/api/v1/privacy-consent` | 4 |
| `/api/v1/payment-order` | 5 |
| `/api/v1/visit-plan` | 15 |
| `/api/v1/plan-item` | 3 |
| `/api/v1/plan-approval` | 4 |
| `/api/v1/plan-history` | 2 |
| `/api/v1/visit-meal-break` | 10 |
| `/api/v1/activity-session` | 9 |
| `/api/v1/visit-record` | 8 |
| `/api/v1/notification` | 7 |
| `/api/v1/google-calendar` | 8 |
| **Total** | **170** |

## Endpoint reference

The table rows below are indexed by method and current route. Descriptions are concise; they are **not** a substitute for request DTO schemas or access-control tests.

### account (6 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/account/get` | All accounts |
| `POST` | `/api/v1/account/add` | Add account |
| `PUT` | `/api/v1/account/update/{id}` | Update account |
| `DELETE` | `/api/v1/account/delete/{id}` | Delete account |
| `POST` | `/api/v1/account/register` | Register an account and visitor profile, then send verification codes |
| `GET` | `/api/v1/account/me` | Current account (no password) |

### auth (6 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/api/v1/auth/verify-email` | Verify the email with the code |
| `POST` | `/api/v1/auth/resend-verification` | Resend the email code |
| `POST` | `/api/v1/auth/verify-phone` | Verify the phone with the WhatsApp code |
| `POST` | `/api/v1/auth/resend-phone-verification` | Resend the phone code |
| `POST` | `/api/v1/auth/forgot-password` | Send a reset code |
| `POST` | `/api/v1/auth/reset-password` | Set a new password with the code |

### visitor-profile (6 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/visitor-profile/get` | All profiles |
| `POST` | `/api/v1/visitor-profile/add` | Add profile |
| `PUT` | `/api/v1/visitor-profile/update/{id}` | Update profile |
| `DELETE` | `/api/v1/visitor-profile/delete/{id}` | Delete profile |
| `GET` | `/api/v1/visitor-profile/my-profile` | My profile |
| `PUT` | `/api/v1/visitor-profile/update-my-profile` | Update my profile |

### visit (14 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/visit/get` | All visits |
| `POST` | `/api/v1/visit/add` | Add visit |
| `PUT` | `/api/v1/visit/update/{id}` | Update visit |
| `DELETE` | `/api/v1/visit/delete/{id}` | Delete visit |
| `POST` | `/api/v1/visit/create` | Create a visit (and its group for FRIENDS / FAMILY) |
| `GET` | `/api/v1/visit/my-visits` | My visits |
| `GET` | `/api/v1/visit/details/{visitId}` | Visit details |
| `PUT` | `/api/v1/visit/update-visit/{visitId}` | Update a visit |
| `PUT` | `/api/v1/visit/cancel/{visitId}` | Cancel a visit |
| `GET` | `/api/v1/visit/preview/{visitId}` | Preview before planning |
| `PUT` | `/api/v1/visit/arrival/{visitId}` | Set the expected arrival time (and meal preferences) |
| `PUT` | `/api/v1/visit/start/{visitId}` | Start the visit (needs a plan) |
| `PUT` | `/api/v1/visit/complete/{visitId}` | Complete the visit |
| `POST` | `/api/v1/visit/reuse/{sourceVisitId}` | Reuse an earlier visit |

### visit-group (9 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/visit-group/get` | All groups |
| `POST` | `/api/v1/visit-group/add` | Add group |
| `PUT` | `/api/v1/visit-group/update/{id}` | Update group |
| `DELETE` | `/api/v1/visit-group/delete/{id}` | Delete group |
| `POST` | `/api/v1/visit-group/add-group/{visitId}` | Add a group to a visit |
| `PUT` | `/api/v1/visit-group/regenerate-code/{visitId}` | Create a new invite code |
| `PUT` | `/api/v1/visit-group/toggle-invitations/{visitId}` | Turn invitations on or off |
| `GET` | `/api/v1/visit-group/qr/{visitId}` | Invite QR code (PNG) |
| `GET` | `/api/v1/visit-group/link/{visitId}` | Shareable invite link |

### visit-member (18 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/visit-member/get` | All members |
| `POST` | `/api/v1/visit-member/add` | Add member |
| `DELETE` | `/api/v1/visit-member/delete/{id}` | Delete member |
| `POST` | `/api/v1/visit-member/join` | Join a visit with the code |
| `PUT` | `/api/v1/visit-member/rsvp/going/{visitId}` | RSVP going |
| `PUT` | `/api/v1/visit-member/rsvp/maybe/{visitId}` | RSVP maybe |
| `PUT` | `/api/v1/visit-member/rsvp/not-going/{visitId}` | RSVP not going |
| `PUT` | `/api/v1/visit-member/update-selected-rides/{visitId}` | Select rides |
| `GET` | `/api/v1/visit-member/preferences/{visitMemberId}` | Read saved ride choices and exclusions |
| `PUT` | `/api/v1/visit-member/preferences/{visitMemberId}` | Save ride choices, disliked features, and exclusions |
| `PUT` | `/api/v1/visit-member/preferences/skip/{visitMemberId}` | Skip preferences |
| `POST` | `/api/v1/visit-member/add-family/{visitId}` | Add saved family members to the visit |
| `POST` | `/api/v1/visit-member/remind/{memberId}` | Remind unanswered visit member |
| `GET` | `/api/v1/visit-member/summary/{visitId}` | Members summary |
| `PUT` | `/api/v1/visit-member/check-in/{visitId}` | Check in at the park |
| `PUT` | `/api/v1/visit-member/review/{visitId}` | Review the visit |
| `DELETE` | `/api/v1/visit-member/leave/{visitId}` | Leave the visit |
| `DELETE` | `/api/v1/visit-member/remove/{visitId}/{memberId}` | Organizer removes a member |

### family-member (5 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/family-member/get/my` | Own family members |
| `GET` | `/api/v1/family-member/get/details/{familyMemberId}` | Family member details |
| `POST` | `/api/v1/family-member/add` | Add family member |
| `PUT` | `/api/v1/family-member/update/{familyMemberId}` | Edit family member |
| `DELETE` | `/api/v1/family-member/delete/{familyMemberId}` | Delete family member |

### ride (12 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/ride/get` | List rides |
| `GET` | `/api/v1/ride/get/{rideId}` | Ride details |
| `POST` | `/api/v1/ride/add` | Create ride |
| `PUT` | `/api/v1/ride/update/{rideId}` | Edit ride |
| `DELETE` | `/api/v1/ride/delete/{rideId}` | Delete ride |
| `GET` | `/api/v1/ride/catalog` | Ride catalogue |
| `GET` | `/api/v1/ride/available` | Available rides |
| `GET` | `/api/v1/ride/wait-times` | Wait-time readings |
| `GET` | `/api/v1/ride/filter` | Filter rides |
| `PUT` | `/api/v1/ride/admin/update-wait/{rideId}` | Admin wait update |
| `PUT` | `/api/v1/ride/admin/update-status/{rideId}` | Admin status update |
| `GET` | `/api/v1/ride/eligibility/{memberId}/{rideId}` | Ride eligibility by member |

### ride-preference (5 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/ride-preference/get/my/preferences` | Own ride preferences |
| `GET` | `/api/v1/ride-preference/get/details/{preferenceId}` | Ride preference details |
| `POST` | `/api/v1/ride-preference/add/{rideId}` | Add ride preference |
| `PUT` | `/api/v1/ride-preference/update/{preferenceId}` | Update ride preference |
| `DELETE` | `/api/v1/ride-preference/delete/{preferenceId}` | Delete ride preference |

### place (6 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/place/get` | List places |
| `GET` | `/api/v1/place/get/{placeId}` | Place details |
| `POST` | `/api/v1/place/add` | Create place |
| `PUT` | `/api/v1/place/update/{placeId}` | Edit place |
| `DELETE` | `/api/v1/place/delete/{placeId}` | Delete place |
| `GET` | `/api/v1/place/by-kind` | Filter places by kind |

### map (8 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/map/get/rides` | Map ride locations |
| `PUT` | `/api/v1/map/link/ride/{rideId}` | Set a ride's map reference |
| `POST` | `/api/v1/map/import/rides` | Import ride map data |
| `POST` | `/api/v1/map/import/places` | Import place map data |
| `GET` | `/api/v1/map/get/places` | Map place locations |
| `GET` | `/api/v1/map/route/{fromType}/{fromId}/{toType}/{toId}` | Route between mapped points |
| `GET` | `/api/v1/map/get/data` | Park map data |
| `GET` | `/api/v1/map/get/walking-context` | Walking routing context |

### privacy-consent (4 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/privacy-consent/get/my/consents` | List own privacy consents |
| `GET` | `/api/v1/privacy-consent/get/details/{consentId}` | Privacy consent details |
| `POST` | `/api/v1/privacy-consent/add` | Grant privacy consent |
| `PUT` | `/api/v1/privacy-consent/revoke/{consentId}` | Revoke consent |

### payment-order (5 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/api/v1/payment-order/confirm/{paymentOrderId}` | Verify payment through Moyasar |
| `GET` | `/api/v1/payment-order/get/my/orders` | Current user's payment orders |
| `GET` | `/api/v1/payment-order/get/details/{paymentOrderId}` | Payment order details |
| `GET` | `/api/v1/payment-order/get/visit/{visitId}` | Orders for visit |
| `POST` | `/api/v1/payment-order/add/{visitId}` | Create payment order |

### visit-plan (15 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/visit-plan/get/map-route/{visitPlanId}` | Get itinerary map route |
| `GET` | `/api/v1/visit-plan/get/plans/{visitId}` | Plans for visit |
| `GET` | `/api/v1/visit-plan/get/details/{visitPlanId}` | Plan details |
| `POST` | `/api/v1/visit-plan/create/individual/{visitId}` | Create individual plan |
| `POST` | `/api/v1/visit-plan/create/group/{visitId}` | Create group plan |
| `DELETE` | `/api/v1/visit-plan/delete/{visitPlanId}` | Delete plan |
| `GET` | `/api/v1/visit-plan/get/my-individual-plan/{visitId}` | Current user's individual plan |
| `GET` | `/api/v1/visit-plan/get/approved-group-plan/{visitId}` | Approved group plan |
| `GET` | `/api/v1/visit-plan/get/pending-group-plan/{visitId}` | Pending group plan |
| `GET` | `/api/v1/visit-plan/meeting-points/{visitId}` | Meeting points |
| `GET` | `/api/v1/visit-plan/next-activity/{visitId}` | Next activity |
| `GET` | `/api/v1/visit-plan/my-subgroup/{visitId}` | Current user's subgroup |
| `POST` | `/api/v1/visit-plan/generate/{visitId}` | Generate AI plan |
| `POST` | `/api/v1/visit-plan/regenerate/group/{visitId}` | Regenerate group plan |
| `POST` | `/api/v1/visit-plan/suggest-replan/{visitId}` | Propose replan |

### plan-item (3 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/plan-item/get/items/{visitPlanId}` | Plan items |
| `GET` | `/api/v1/plan-item/get/details/{planItemId}` | Plan item details |
| `PUT` | `/api/v1/plan-item/update/preferences` | Update plan item preferences |

### plan-approval (4 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/plan-approval/get/{visitPlanId}` | Get approval status |
| `POST` | `/api/v1/plan-approval/create/{visitPlanId}` | Request approval |
| `PUT` | `/api/v1/plan-approval/approve` | Approve plan or proposal |
| `PUT` | `/api/v1/plan-approval/reject` | Reject plan or proposal |

### plan-history (2 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/plan-history/my-history` | Get own generation history |
| `GET` | `/api/v1/plan-history/details/{planHistoryId}` | Generation history details |

### visit-meal-break (10 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/visit-meal-break/get/visit/{visitId}` | Meal breaks for visit |
| `POST` | `/api/v1/visit-meal-break/add/{visitId}/{visitMemberId}` | Add meal break |
| `PUT` | `/api/v1/visit-meal-break/update/{visitId}/{mealBreakId}` | Update meal break |
| `DELETE` | `/api/v1/visit-meal-break/delete/{visitId}/{mealBreakId}` | Delete meal break |
| `GET` | `/api/v1/visit-meal-break/get/my/{visitId}` | Own meal breaks |
| `POST` | `/api/v1/visit-meal-break/add/shared/{visitId}` | Add group meal break |
| `DELETE` | `/api/v1/visit-meal-break/delete/shared/{visitId}/{breakKey}` | Delete shared meal break |
| `PUT` | `/api/v1/visit-meal-break/update/shared/{visitId}/{breakKey}` | Update shared meal break |
| `PUT` | `/api/v1/visit-meal-break/preferences/{visitId}` | Save meal preferences |
| `PUT` | `/api/v1/visit-meal-break/restaurant/{visitId}/{breakKey}` | Select restaurant |

### activity-session (9 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/activity-session/history/{visitId}` | Activity history |
| `GET` | `/api/v1/activity-session/current/{visitId}` | Current activity |
| `POST` | `/api/v1/activity-session/start/manual/{visitMemberId}/{planItemId}` | Start activity manually |
| `POST` | `/api/v1/activity-session/complete/{activitySessionId}` | Complete activity |
| `PUT` | `/api/v1/activity-session/rate/{activitySessionId}` | Rate activity |
| `PUT` | `/api/v1/activity-session/location/{visitId}` | Update GPS location |
| `POST` | `/api/v1/activity-session/start/gps/{visitMemberId}/{planItemId}` | Start activity with GPS |
| `POST` | `/api/v1/activity-session/start/manual/family/{planItemId}` | Start family activity |
| `POST` | `/api/v1/activity-session/complete/family/{activitySessionId}` | Complete family activity |

### visit-record (8 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/visit-record/get/my-records/{visitId}` | Personal visit records |
| `POST` | `/api/v1/visit-record/complete/{visitId}` | Record completed activity |
| `POST` | `/api/v1/visit-record/skip/{visitId}` | Record skipped activity |
| `PUT` | `/api/v1/visit-record/update/complete` | Correct completed status |
| `PUT` | `/api/v1/visit-record/update/skip` | Correct skipped status |
| `PUT` | `/api/v1/visit-record/rate` | Rate record |
| `GET` | `/api/v1/visit-record/visit-summary/{visitId}` | Visit summary |
| `GET` | `/api/v1/visit-record/progress/{visitId}` | Visit progress |

### notification (7 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/notification/get/my-notifications` | Current user's notifications |
| `GET` | `/api/v1/notification/get/details/{notificationId}` | Notification details |
| `DELETE` | `/api/v1/notification/delete/{notificationId}` | Delete notification |
| `PUT` | `/api/v1/notification/read/{notificationId}` | Mark notification read |
| `GET` | `/api/v1/notification/unread-count` | Unread notifications count |
| `PUT` | `/api/v1/notification/respond-delay/{notificationId}` | Respond to delay prompt |
| `POST` | `/api/v1/notification/check-delay/{activitySessionId}` | Check activity delay |

### google-calendar (8 endpoints)

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/google-calendar/manual/{visitId}` | Create manual Google Calendar link |
| `GET` | `/api/v1/google-calendar/apple/{visitId}` | Download ICS calendar file |
| `GET` | `/api/v1/google-calendar/oauth/connect` | Start Google OAuth |
| `GET` | `/api/v1/google-calendar/oauth/callback` | OAuth callback |
| `GET` | `/api/v1/google-calendar/oauth/status` | Check calendar connection |
| `POST` | `/api/v1/google-calendar/sync/{visitId}` | Sync visit plan |
| `DELETE` | `/api/v1/google-calendar/sync/cancelled/{visitId}` | Remove cancelled visit calendar event |
| `DELETE` | `/api/v1/google-calendar/oauth/disconnect` | Disconnect calendar |

## Example requests

### List rides (public catalogue)

```http
GET /api/v1/ride/catalog HTTP/1.1
Host: your-backend.example
Accept: application/json
```

### Find a visit

```http
GET /api/v1/visit/details/123 HTTP/1.1
Host: your-backend.example
Accept: application/json
Cookie: JSESSIONID=<session-cookie>
```

### Generate a plan

```http
POST /api/v1/visit-plan/generate/123 HTTP/1.1
Host: your-backend.example
Content-Type: application/json
Cookie: JSESSIONID=<session-cookie>
```

The last example illustrates the route and authentication context only. Consult `GeneratePlanDtoIn` for any required JSON body; generation also depends on visit eligibility, readiness, and payment rules.

## Key business contracts

| Area | Contract |
|---|---|
| Visit types | `SOLO`, `FRIENDS`, `FAMILY` |
| Ride priorities | `MUST_RIDE`, `PREFERRED`, `OPTIONAL` |
| Plan generation | AI output is checked against server-side eligibility and scheduling rules |
| Approval | Plans and replanning proposals require applicable approval rules |
| Billing | Free-visit allowance and payment verification enforced server-side |
| Payment gateway | Moyasar payment checked from backend; frontend result alone is not proof |
| Calendar | Google OAuth / synchronization, plus manually generated links and Apple `.ics` files |
| Notifications | In-app and integrations; delivery depends on credentials and external services |
| Ride waits | Historical or estimated waits must not be represented as confirmed live times |

## External systems

| System | Integration role |
|---|---|
| OpenAI API | Itinerary generation and replanning |
| Aladhan API | Prayer schedule lookup |
| Google Calendar API | OAuth and approved itinerary sync |
| Gmail SMTP | Email verification and account notifications (configuration in repository) |
| UltraMsg | WhatsApp verification and messages |
| Moyasar | Server-side payment confirmation |
| n8n webhook | Notification orchestration, subject to runtime configuration |

> **Brevo note:** Current repository `EmailService` uses Spring `JavaMailSender`, and checked configuration points to Gmail SMTP. Do not describe Brevo as the active backend provider without deployment-level verification.

## Endpoint authorship (separate Git audit)

The controller owner or model creator is **not** automatically credited with all routes in that controller. The agreed attribution policy is:

1. Model ownership goes to the developer who first introduced the model in Git.
2. An endpoint is credited to the developer who **first implemented that route**, including routes inside someone else’s controller.
3. Later changes, merge fixes and cross-module integrations belong in additional contributions, not duplicate endpoint counts.

**Personal endpoint totals are intentionally not asserted in this document.** A complete, route-by-route history audit is needed before publishing counts for Abdulaziz, Mohammed and Nawaf. This avoids presenting controller ownership totals as personal work.

## Source of truth

- Backend controllers: [`src/main/java/com/example/sikka/Controller/`](https://github.com/Abdulaziz-Shafae/SIKKA-final/tree/main/src/main/java/com/example/sikka/Controller)
- Request DTOs: [`DtoIn/`](https://github.com/Abdulaziz-Shafae/SIKKA-final/tree/main/src/main/java/com/example/sikka/DtoIn)
- Response DTOs: [`DtoOut/`](https://github.com/Abdulaziz-Shafae/SIKKA-final/tree/main/src/main/java/com/example/sikka/DtoOut)
- Security: [`SecurityConfig.java`](https://github.com/Abdulaziz-Shafae/SIKKA-final/blob/main/src/main/java/com/example/sikka/Config/SecurityConfig.java)
- Backend repository: https://github.com/Abdulaziz-Shafae/SIKKA-final

---

*Documentation is for review only. It has not been pushed to GitHub.*
