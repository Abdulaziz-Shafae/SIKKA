# Mohammed Turki — Contributions to SIKKA

> [Main project README](https://github.com/Abdulaziz-Shafae/SIKKA#readme)

**Core entities:** Account, VisitorProfile, VisitGroup, Visit, VisitMember. FamilyMember workflows are used in this flow, but Nawaf originally created that Model.

**Main work:** Moyasar payment integration, payment order creation and confirmation, backend payment verification, registration and account/profile management, email and phone verification, password recovery, individual/friends/family visit creation, group invitations, RSVP, ride selections, visit preview, check-in, and visit lifecycle management.

**Integrations:** Gmail SMTP for email verification, UltraMsg for WhatsApp verification and ZXing for invitation QR codes. Implemented server-side Moyasar payment verification and integrated paid-visit eligibility into the visit workflow.

**Additional contributions:** session-security configuration, account and visit validation, frontend-facing visit contracts, and flow-level tests. Work shared across modules is not independently attributed here.

**Shared work note:** Several visit and member endpoints were introduced by Abdulaziz in Controllers associated with Mohammed’s models. Model ownership is kept separate from endpoint authorship.
