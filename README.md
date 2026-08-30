# Klarblatt

Klarblatt is an accessibility-first RSS/Atom reader for people who are blind,
losing their sight, or want a clearer reading experience. It uses large,
high-contrast type, topic-based discovery, key points, read-aloud, and saved
articles. It is server-rendered and remains usable without JavaScript.

Klarblatt is a sibling of [Extrablatt](https://github.com/Philipp0205/kindle-rss),
the Kindle-first reader at <https://reader.extrablatt.app>. Klarblatt has no
edition switch and never serves the paged Kindle UI as its product experience.

## Features

- E-mail/password accounts with verification and password reset
- Isolated topics, sources, articles, display preferences, and saved articles
- Ready-made topics for following multiple trusted sources at once
- RSS/Atom discovery from a normal website address
- Large type, four high-contrast themes, adjustable line and letter spacing,
  and serif or sans-serif fonts
- Extractive key points from article headings, lists, quotations, and lead text
- Browser read-aloud with voice and speed selection
- Scheduled and manual feed refresh with readable article extraction
- Optional newsletter inboxes
- Optional Send-to-Kindle, shown only after an account adds a Kindle address
- Per-account limits, authentication rate limiting, and an admin dashboard

The design rationale is documented in [`docs/design.md`](docs/design.md).

## Requirements

- Java 21
- Maven 3.9+ or the included Maven Wrapper
- PostgreSQL 16+ (17 recommended)
- An SMTP provider and verified sender domain for account e-mail

## Run locally

1. Create a PostgreSQL database and user.
2. Copy `.env.example` to `.env` and set the database and mail values.
3. Export those variables with your preferred dotenv tooling.
4. Start the app:

```bash
./mvnw spring-boot:run
```

Open <http://localhost:8080>. After login, Klarblatt always opens `/topics`.

Important variables:

| Variable | Purpose |
|---|---|
| `DATABASE_URL` | JDBC URL |
| `DATABASE_USER`, `DATABASE_PASSWORD` | PostgreSQL credentials |
| `APP_PUBLIC_URL` | One public application URL used in account e-mails |
| `MAIL_FROM` | Verified sender for account and optional Kindle mail |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | SMTP connection |
| `REMEMBER_ME_KEY` | Secret used to sign remember-me tokens |
| `ADMIN_EMAILS` | Comma-separated administrator account addresses |
| `NEWSLETTER_INBOUND_DOMAIN`, `NEWSLETTER_INBOUND_SECRET` | Optional newsletter ingestion |

There is no accessibility-domain setting. One app domain serves Klarblatt.

## Tests

```bash
./mvnw -B test
```

The test suite covers article extraction, sanitization, outbound request safety,
services, persistence, security, and the complete accessible MVC experience.
Tests use embedded PostgreSQL where persistence is required.

## Optional Send-to-Kindle

Readers who want Kindle delivery can add a Send-to-Kindle address under
**Your account**. Amazon must also list `MAIL_FROM` under the account's approved
Personal Document E-mail addresses. Readers without a Kindle address never see
the article delivery action.

## Optional newsletters

Set `NEWSLETTER_INBOUND_DOMAIN` and `NEWSLETTER_INBOUND_SECRET` to give each
account an inbound newsletter address. Point an inbound mail provider such as
Postmark at:

```text
https://<your-domain>/inbound/newsletters?secret=<NEWSLETTER_INBOUND_SECRET>
```

Each sender becomes a source and each message becomes an article. Leaving the
variables unset hides this feature.

## Deploy with Docker

Fill `.env`, including one application `DOMAIN`, then run:

```bash
docker compose -f deploy/docker-compose.yml --env-file .env up -d --build
```

The stack contains:

- `app`: the Spring Boot application
- `postgres`: internal PostgreSQL 17
- `caddy`: TLS termination and reverse proxy for `DOMAIN`

`MARKETING_DOMAIN` is optional and serves the static `marketing/` directory.
It does not create a second reader domain. The marketing links use
`https://read.klarblatt.app`; change those links if your production app domain
differs.

When a host already provides TLS, use:

```bash
docker compose -f deploy/docker-compose.yml \
  -f deploy/docker-compose.host-proxy.yml \
  --env-file .env up -d --build
```

Then proxy to `127.0.0.1:${APP_HTTP_PORT:-8090}`. An example is available in
`deploy/host-caddy-site.example`.

## Deploy on Railway

Create a Railway PostgreSQL service, deploy this repository using the root
`Dockerfile`, and configure:

```text
SPRING_PROFILES_ACTIVE=production
DATABASE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DATABASE_USER=${{Postgres.PGUSER}}
DATABASE_PASSWORD=${{Postgres.PGPASSWORD}}
APP_PUBLIC_URL=https://read.klarblatt.app
REMEMBER_ME_KEY=<long random value>
MAIL_FROM=noreply@your-domain.example
SMTP_HOST=smtp.resend.com
SMTP_PORT=587
SMTP_USERNAME=resend
SMTP_PASSWORD=<provider credential>
```

Attach only the Klarblatt application domain to the app service. The optional
static marketing site can be deployed from `marketing/` as a separate service.

## VPS deployment

`deploy/deploy.sh` synchronizes the repository and runs the Compose stack on a
VPS. Set `DOMAIN` to the single Klarblatt application host, for example
`read.klarblatt.app`. Keep production `.env` files and SSH keys outside Git.

If the server owns the only production `.env`, set `ENV_FILE` to a nonexistent
path so deployment does not overwrite it.

## Security and architecture

- CSRF protection is enabled for browser actions.
- Feed and article HTML is sanitized before rendering.
- Outbound HTTP rejects private, loopback, link-local, multicast, CGNAT, and ULA
  destinations and caps response size and duration.
- Redirects from forms are restricted to same-app relative paths.
- Production session and remember-me cookies are Secure.
- The app expects one active instance unless scheduling and rate limits are moved
  to shared coordination.

## Stack

Spring Boot 3.5, Java 21, Thymeleaf, Spring Security, JDBC, Flyway, PostgreSQL,
ROME, Readability4J, jsoup, and epub4j.
