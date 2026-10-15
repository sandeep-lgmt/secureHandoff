# SecureHandoff

A dead-man's-switch API for sharing sensitive information safely. Owners store encrypted
"access packets". If the owner stops checking in, trusted contacts must independently
confirm before anything is released.

**Live API:** https://securehandoff.onrender.com/actuator/health
(Hosted on a small instance, so the first request may take about a minute.)

## How it works

1. Owner registers, stores packets (AES-256-GCM encrypted), and sets a check-in schedule.
2. Owner invites trustees by email.
3. If the owner misses a check-in, an hourly job creates a release request and emails trustees.
4. Trustees confirm; packets are decrypted only once enough trustees agree (quorum).
5. Every action is written to a hash-chained audit log.

## Tech stack

Java 25, Spring Boot 3.5, Spring Security + JWT, JPA/Hibernate, MySQL (TiDB Cloud),
Redis, Docker, Render, Brevo email API.

## Security decisions

- Packet content encrypted at rest with AES-256-GCM; key held outside the code.
- Quorum release: no single trustee can unlock data.
- Login rate limiting; passwords hashed with BCrypt.
- Row locking so simultaneous confirmations can't miss the quorum.
- No secrets in the repository; configuration comes from environment variables.

## API overview

| Endpoint                                           | Purpose                              |
| -------------------------------------------------- | ------------------------------------ |
| POST /api/auth/register, /login                    | Accounts and JWT login               |
| /api/packets                                       | Create, list, update, delete packets |
| /api/checkin/setup, /api/checkin                   | Configure and perform check-ins      |
| /api/trustees/invite, /accept                      | Manage trustees                      |
| /api/release/pending, /{id}/confirm, /{id}/packets | Release flow                         |

## Run locally

Set DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET and PACKET_SECRET, then `./mvnw spring-boot:run`.
