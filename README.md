# SIKKA — Railway / Brevo Deployment Branch

> **Deployment-specific branch.** For the project overview, architecture, contributors, and API reference, see the [main README](https://github.com/Abdulaziz-Shafae/SIKKA#readme).

## Why this branch is separate

`deployment/brevo-railway` was kept separate from `main` to support and validate a Railway-oriented release, especially **email delivery over Brevo's HTTPS API** when outbound SMTP connectivity is restricted. This is a deployment variant, **not** a fourth contributor branch or the project's canonical development source.

The two branches have since diverged: this branch contains deployment and follow-up changes not present in `main`, while `main` contains newer changes absent here. **Do not reset, force-push, or merge it into `main` without reviewing both sides.** If Railway is configured to deploy this branch, changing it can trigger a live deployment.

## What is different here

| Area | Railway branch behavior |
|---|---|
| Email | `EmailService` uses Brevo's HTTPS `/v3/smtp/email` endpoint **when `BREVO_API_KEY` is set**; otherwise falls back to Spring `JavaMailSender` (SMTP). |
| Email sender | `MAIL_FROM` configures the sender address; fallback is `MAIL_USERNAME`. The sender must be authorized by the configured provider. |
| Deployment | Docker and Railway configuration remain in this branch. Production profile lives at `src/main/resources/application-prod.properties`. |
| Follow-up fixes | This branch also includes registration notification/cleanup and planning, arrival, prayer and meal-flow changes. These are **not all Railway-only changes** and need a separate comparison with `main`. |

**Important:** A configured provider or implemented method does not by itself prove that live email, prayer scheduling, or the deployment currently succeeds. Verify runtime logs, environment variables, and integration tests separately.

## Railway configuration

Configure secrets in Railway's environment settings; never commit real credentials. Email-related settings:

```dotenv
BREVO_API_KEY=...       # enables HTTPS API delivery
MAIL_FROM=...           # provider-authorized sender email
MAIL_USERNAME=...       # SMTP fallback user / default sender
MAIL_PASSWORD=...       # needed only for SMTP fallback
SPRING_PROFILES_ACTIVE=prod
```

Other required database, OpenAI, WhatsApp, calendar, and payment variables depend on the deployed features and existing `application.properties` configuration.

## Safe maintenance and cleanup

1. **Check Railway's connected GitHub branch before changes.** This README does not assert the live service is deploying from this branch.
2. Keep `Dockerfile`, `railway.json`, `application-prod.properties`, deployment-specific email code, and associated tests until deployment is verified.
3. For cleanup or synchronization, compare against `main` and classify each change as deployment-only, already superseded, or still needed in both branches.
4. Prefer a reviewed pull request for code changes. Run Maven tests and verify Railway deployment, email delivery, registration, and plan generation before merging or removing files.

## Links

- [General backend README (`main`)](https://github.com/Abdulaziz-Shafae/SIKKA#readme)
- [API documentation (`main`)](https://github.com/Abdulaziz-Shafae/SIKKA/blob/main/docs/API-DOCUMENTATION.md)
- [Live SIKKA website](https://sikka.up.railway.app/#home)
