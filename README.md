
# Shopping Cart Web App – Self‑Contained Fullstack Microservices

## Repository Purpose (What & Why)
This repository is a deliberately simplified, batteries‑included demo of a microservices commerce slice (products + cart + users) that prioritizes an ultra‑low friction developer experience:

Focus goals:
* Clone → run in **two commands** (`./setup.sh` then `./run.sh`).
* Zero external infra dependencies (no external DB, no discovery server) while still showing realistic patterns (separate bounded contexts, gateway, DTOs, security layer, seed data, React frontend).
* Deterministic, idempotent seeding so every fresh startup has working demo credentials & sample products.
* Clear upgrade path: Appendix shows how to re‑enable service discovery if you need dynamic scaling later.

Intended audience:
* Engineers evaluating microservice partitioning & gateway patterns without wrestling infra.
* New hires / workshop participants needing a quick, consistent environment.
* People wanting a reference for structuring a small Spring Boot + React multi‑service project with shared build/run scripts.

Non‑goals:
* Full production hardening (JWT/OAuth2, distributed tracing, circuit breakers, persistent RDBMS) – those are intentionally deferred and listed in the Roadmap.
* Complex domain modeling: pricing, promotions, inventory reservations, etc. kept intentionally lean.

## What Changed Versus the Original Repository
| Area | Original State | Updated / Current State | Rationale |
|------|----------------|-------------------------|-----------|
| Service Discovery | Eureka server + clients | Removed (static port routing via gateway) | Reduce moving parts; instant startup; easier demos |
| Startup Complexity | Multiple commands, potential race between frontend build & services | Single orchestrator `run.sh` with readiness polling & optional flags | Predictable DX; uniform logs & health status |
| Database Layer | Mixed (original doc referenced MySQL) | Unified H2 file DB per service (persisted under `data/`) | No external installs required; fast reset |
| Data Seeding | SQL / ad‑hoc bootstrap causing duplicate key issues | Java `CommandLineRunner` seeders (idempotent) for Users & Inventory | Deterministic & safe re-runs |
| Cart Repository ID | Inconsistent ID type (string vs UUID) | Normalized to `UUID` | Type safety & clarity |
| Pricing Seed Types | Mixed numeric types (double vs BigDecimal) | BigDecimal everywhere | Monetary correctness |
| Frontend Cart State | Separate isolated hooks per page (Add felt like "no change") | Shared `CartContext` provider | Immediate UI reflection; single source of truth |
| Build Artifacts | Stray `target 2` directories caused `mvn clean` issues | Cleaned & strengthened `.gitignore` | Stable builds / clean working tree |
| Discovery Service Module | Present | Kept as historical (documented removal) | Narrative clarity + optional re‑enable path |
| Auth | Basic Auth + manual principal decode | Still Basic (manual extract) | Simplicity; TODO for JWT/Principal improvements |
| Observability & Docs | Minimal | Expanded README (purpose, comparison, architecture) | Onboarding efficiency |

### Summary of Improvements
The repository now optimizes for (1) fast onboarding, (2) reproducible runs, (3) minimal cognitive overhead, while cleanly separating concerns so advanced features can be layered in later.

## Quick Start (Two Commands)

### 0. Prerequisites
Install (ensure on `PATH`):
* JDK 17+ (Java 21 also works; tested with 21)
* (Optional) Maven 3.8+ (Maven Wrapper included; global Maven not required)
* Node.js 18+ (for frontend build)
* (Optional) Docker + Docker Compose plugin (for `--docker` mode)

No external database (MySQL/Postgres/etc.) is required. Each service uses its own embedded H2 file database under `./data/`.

Clone the repo:
```bash
git clone https://github.com/<your-org-or-user>/Shopping-Cart-Web-App-self-contained.git
cd Shopping-Cart-Web-App-self-contained
```

If you're on macOS/Linux (or WSL) and see a permission error like `permission denied: ./setup.sh`, give the scripts the executable bit once:
```bash
chmod +x setup.sh run.sh scripts/smoke.sh
```
PowerShell users (Windows) can run the `.ps1` scripts directly; if you get an execution policy warning run:
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy RemoteSigned
```

### 1. Setup (downloads & prepares everything)
macOS/Linux:
```bash
./setup.sh        # or: bash setup.sh (if you didn't chmod yet)
```
Windows (PowerShell):
```powershell
./setup.ps1
```

### 2. Run (starts full stack)
macOS/Linux:
```bash
./run.sh            # builds missing jars then launches them
```
Windows (PowerShell):
```powershell
./run.ps1
```

Common flags:
```bash
--rebuild         # Force repackage all service jars before starting
--force-restart   # Kill anything already bound to the target ports
--docker          # Use docker-compose mode instead of local JVM processes
--open            # (local mode) Auto-open the gateway in your browser after readiness check
```

Examples:
```bash
./run.sh --rebuild
./run.sh --force-restart
./run.sh --docker --build   # container mode (optional)
./run.sh --force-restart --open  # restart everything and open browser
```

### Startup Readiness Output
After launching services locally, `run.sh` prints a live readiness table, polling lightweight root info endpoints (JSON metadata) to confirm each service is accepting requests:

| Service  | Endpoint polled            | Notes |
|----------|----------------------------|-------|
| inventory| http://localhost:8081/     | Root returns service name, version, timestamp |
| user     | http://localhost:8082/     | Same uniform root info payload |
| cart     | http://localhost:8083/     | Unprotected root (avoids auth for readiness) |
| gateway  | http://localhost:8080/api/v1/items | Uses product list to ensure routing + frontend assets are ready |

Status codes shown:
* OK – Successful response (HTTP 200) within timeout
* STARTING – Last probe failed but timeout window not yet exceeded
* TIMEOUT – Exceeded allotted wait (script keeps supervising existing processes)
* SKIPPED – Port was already in use (pre-existing instance adopted rather than restarted)

When all services reach OK (or any TIMEOUT threshold is hit), the table stops updating and (if you passed `--open`) your default browser is opened to the gateway.

Tip: If a service shows TIMEOUT, inspect its log, e.g. `tail -f logs/cart-service.log`. You can safely re-run `./run.sh` (it will attach to running services unless you specify `--force-restart`).

### Docker Mode (optional)
Add `--docker` (and optionally `--build` on first run) for container mode:
```bash
./run.sh --docker --build
```
PowerShell:
```powershell
./run.ps1 --docker --build
```

Then open: http://localhost:8080

Seed demo credentials (Basic Auth for protected endpoints):
```
Username: demo
Password: demo
```

The user-service deterministically ensures three users exist on every startup (system, admin, demo – all with password 'demo'). If you delete the H2 files in `data/` they will be recreated. This guarantees Basic Auth works even after wiping the database.

Smoke test after startup:
```bash
bash scripts/smoke.sh
```

## Frontend Overview
The frontend lives under `frontend/` and is a Vite + React application compiled to static assets and copied into the API Gateway's `static` resources folder during `./run.sh`.

Key pieces:
* Products Page: Fetches item catalog from `GET /api/v1/items` (public).
* Cart Page: Displays `GET /api/v1/cart-items` and `GET /api/v1/cart-items/summary` (requires Basic Auth).
* Shared State: `CartContext` wraps routes to provide a synchronized cart state across pages. Adding an item triggers a POST then refresh of shared context so all consumers update instantly.
* Auth: Implemented with browser Basic Auth header (prompt or manual). Future upgrade path: JWT access/refresh tokens.
* Tooling: Vite for fast dev builds; production build executed automatically by `run.sh` (no manual `npm run build` needed).

Frontend Flow Example:
1. User opens gateway root (served by Spring Boot static resources).
2. Products page mounts, loads items, displays Add buttons.
3. Clicking Add executes POST `/api/v1/cart-items` with `{ itemCode, qty: 1 }` (backend injects username).
4. Context re-fetches cart lines & summary; Cart badge (if implemented) updates immediately.

## Backend Overview
Microservices (each independent Spring Boot application) expose cohesive capabilities:
* Inventory Service (`8081`): Item catalog with code, description, quantity, price. Seeds via `InventorySeedConfig` (idempotent).
* User Service (`8082`): Demo users (system/admin/demo) created in `UserSeedConfig` (all password = `demo`). Basic Auth user lookup leveraged by gateway.
* Cart Service (`8083`): Persists cart lines per user with price snapshots; endpoints for list, summary, add, remove. Uses a REST call to inventory to enrich line pricing and store snapshot.
* API Gateway (`8080`): Single entrypoint; static asset host for frontend; routes API calls to internal services; applies Basic Auth security rules (public inventory list, protected cart/user endpoints). Static routing (no discovery) for clarity.

Design Choices:
* Root (`/`) info endpoints provide lightweight readiness probes (service name, timestamp) to speed script polling.
* Deterministic seeding uses `CommandLineRunner` with existence checks instead of fragile SQL scripts.
* Monetary values handled with `BigDecimal` to prevent floating precision errors.
* Context state in frontend avoids excessive network chatter and prevents UI desync.

Extensibility Paths:
* Reintroduce discovery (Appendix A) and expand to dynamic scaling.
* Swap Basic Auth (demo) for JWT once auth complexity is desired.
* Add Actuator + metrics for production readiness.
* Introduce caching (Redis) for hot catalog reads.

## Troubleshooting
| Symptom | Cause | Fix |
|---------|-------|-----|
| `permission denied: ./setup.sh` | Missing execute bit on Unix | `chmod +x setup.sh run.sh` or use `bash setup.sh` |
| `command not found: mvn` | Global Maven not installed | Safe to ignore: scripts fall back to per-service `mvnw` |
| Cart 500 on `/api/v1/cart-items` | (Pre-1.1.1) Gateway user lookup lacked password field | Update to >=1.1.1 or pull latest; restart with `./run.sh --force-restart` |
| Skipped service (port in use) | Previous instance still running | Use `./run.sh --force-restart` to kill & restart |
| Want a clean rebuild | Incremental jar not refreshed | Run with `--rebuild` |
| Old plugin exit 143 lines | Legacy spring-boot:run noise | Eliminated: jars now launched directly |
| `docker: command not found` when using `--docker` | Docker not installed | Install Docker Desktop / Engine or run without `--docker` |
| Frontend 404 for assets | Gateway started before build finished (rare) | Re-run `./run.sh` or manually `npm run build` then restart gateway |
| Port already in use | Another process occupying required port | Adjust ports in `.env` then rerun setup/run |
| PowerShell script blocked | Execution policy restriction | `Set-ExecutionPolicy -Scope Process RemoteSigned` |
| Tests fail on first run due to missing deps | `npm install` not finished | Re-run `./setup.sh` (ensures dependencies) |

## Features
* Two-command startup (`setup` then `run`) – no manual multi-step orchestration
* Direct static routing via API Gateway (no discovery layer required)
* Gateway routing + CORS + Basic Auth (lookup endpoint: `/api/v1/users/username/{username}`) – demo-only; password hash exposed upstream strictly for development
* Inventory management (seeded products)
* Cart service with price snapshots & summary endpoint
* User service with hashed passwords & deterministic seed users
* React + Vite frontend (products & cart) with shared cart context
* Embedded H2 file DBs per service (persistent across restarts; easy reset)
* Optional Docker / docker-compose mode
* Fallback logic when Docker absent
* Persistent dev data stored under `./data` (safe to delete); excluded from VCS
* Readiness orchestration & table output for visibility

## Architecture Overview
Services (all independent H2 databases):
| Service | Port | Purpose |
|---------|------|---------|
| api-gateway | 8080 | Entry point, static frontend, routing |
| inventory-service | 8081 | Products, pricing |
| user-service | 8082 | Users & roles |
| cart-service | 8083 | Cart items & summaries |

Frontend static build is copied into the gateway (`/static`) during `./run.sh` before the gateway jar is packaged/launched.

## Scripts
| Script | Purpose |
|--------|---------|
| `setup.sh` / `setup.ps1` | Install dependencies (Maven offline, npm install, create `.env`) |
| `run.sh` / `run.ps1` | Start all services (local or `--docker`) |
| `scripts/smoke.sh` | Simple curl-based availability check |

Environment toggle: set `DOCKER_MODE=true` in `.env` or pass `--docker`.

## Testing
Backend: JPA repository test + cart service unit test (mocked inventory). Frontend: Vitest + React Testing Library sample rendering test.

Run all tests:
```bash
mvn -q -DskipTests=false test  # (run in each service directory)
cd frontend && npm test
```

## Risk & Fallback Considerations
| Risk | Mitigation / Fallback |
|------|-----------------------|
| Docker absent | `run.sh --docker` auto-detects and reverts to local Maven mode |
| Compose command variant | Tries `docker compose`, then `docker-compose`, else local |
| Port conflicts | Override in `.env` (GATEWAY_PORT, INVENTORY_PORT, etc.) |
| Slow first build | `setup.sh` pre-fetches Maven deps (dependency:go-offline) |
| Missing frontend dependencies | `setup.sh` runs `npm install` |
| Data loss between runs | H2 file DB persists in `./data` (or Docker volumes) |

## Original System Design & Principles
The original detailed system design, principles, and references are preserved below for context.

- [System Design](#system-design)
  - [1. Requirements](#1-requirements)
  - [2. High-level design](#2-high-level-design)
  - [3. Defining data model](#3-defining-data-model)
    - [Cart Service](#cart-service)
  - [4. Detailed design](#4-detailed-design)
    - [API Gateway](#api-gateway)
    - [Discovery Service](#discovery-service)
- [Software development principles](#software-development-principles)
  - [KISS (Keep It Simple Stupid)](#kiss-keep-it-simple-stupid)
  - [YAGNI (You aren't gonna need it)](#yagni-you-arent-gonna-need-it)
  - [Separation of Concerns](#separation-of-concerns)
  - [DRY](#dry)
  - [Code For The Maintainer](#code-for-the-maintainer)
  - [Minimise Coupling](#minimise-coupling)
  - [Inversion of Control](#inversion-of-control)
  - [Single Responsibility Principle](#single-responsibility-principle)
- [Design Patterns](#design-patterns)
- [Application default configuration](#application-default-configuration)
- [How to run the application](#how-to-run-the-application)
  - [Setup development workspace](#setup-development-workspace)
  - [Run a microservice](#run-a-microservice)
- [Project folder structure and Frameworks, Libraries](#project-folder-structure-and-frameworks-libraries)
  - [Project folder structure](#project-folder-structure)
  - [Frameworks and Libraries](#frameworks-and-libraries)
- [Version](#version)
- [License](#license)
- [References](#references)
- [Contact Details](#contact-details)

## System Design

### 1. Requirements
1. Created an Auth instance using Spring security And create another service instance that can be cart service.
2. Can add item to the shopping cart
3. Can remove item from the shopping cart.
4. Manage communication between services that created.
5. Can create roles and there is an admin roles to add items to the site. (To manage the inventory)
6. Used Mysql database manage with ORM

### 2. High-level design
At a high-level, we need some following services (or components) to handle above requirements:

![High Level Design](external-files/HighLevelDesign.png)

- **Inventory Service**: Add item to the site, only admin role can manage the inventory.
- **User Service**: manage all users service and create new users
- **Cart Service**: manages customers shopping carts with CRUD operations.
- **API Gateway**: Route requests to multiple services using a single endpoint. This service allows us to expose multiple services on a single endpoint and route to the appropriate service based on the request.

### 3. Defining data model
   In this part, we describe considerations for managing data in our architecture. For each service, we discuss data schema and datastore considerations.

   In general, we follow the basic principle of microservices is that each service manages its own data. Two services should not share a data store.
   
![High Level Design](external-files/define-data.png)

#### Cart Service
The Cart service stores information about cart of the customers. The storage requirements for the Shopping Cart Service are:
- Short-term storage. Each customer will have their own shopping cart and only one shopping cart at the moment. After customer checkout, the shopping cart data will be cleared.
- Need retrieve/lookup shopping cart data quickly and update shopping cart data quickly.

### 4. Detailed design
#### API Gateway
We need API Gateway for following reasons:
- When a client needs to consume multiple services, setting up a separate endpoint for each service and having the client manage each endpoint can be challenging. Each service has a different API that the client must interact with, and the client must know about each endpoint in order to connect to the services. If an API changes, the client must be updated as well. If we refactor a service into two or more separate services, the code must change in both the service and the client.
- Simplify application development by moving shared service functionality, such as the use of SSL certificates, from other parts of the application into the gateway. Other common services such as authentication, authorization, logging, monitoring, or throttling can be difficult to implement and manage across a large number of deployments. It may be better to consolidate this type of functionality, in order to reduce overhead and the chance of errors. Simpler configuration results in easier management and scalability and makes service upgrades simpler.
- Provide some consistency for request and response logging and monitoring.

#### (Removed) Discovery Service
Originally the system used Eureka (Spring Cloud Netflix). For a lean “clone & run” developer experience it was removed and the gateway now forwards directly to fixed service ports. See Appendix A to restore it.


## Software development principles
### KISS (Keep It Simple Stupid)
- Most systems work best if they are kept simple rather than made complex.
- Less code takes less time to write, has less bugs, and is easier to modify.
- > The best design is the simplest one that works - Albert Einstein.

**What applied:** Keep system design and the implementation code simple

### YAGNI (You aren't gonna need it)
- Don't implement something until it is necessary.
- Any work that's only used for a feature that's needed tomorrow, means losing effort from features that need to be done for the current iteration.

**What applied:** Always implement things when we actually need them, never when we just foresee that we need them.

### Separation of Concerns
- Separating a system into multiple distinct microservices, such that each service addresses a separate concern (inventory, user, cart...).
- In each service, break program functionality into separate layers.
- AOP to separate of cross-cutting concerns.

### DRY
- Put business rules, long expressions, if statements, math formulas, metadata, etc. in only one place.

### Code For The Maintainer
- Maintenance is by far the most expensive phase of any project.
- Always code as if the person who ends up maintaining your code is a violent psychopath who knows where you live.
- Always code and comment in such a way that if someone a few notches junior picks up the code, they will take pleasure in reading and learning from it.

**What applied:** Comprehensive documentation, make the code clean, add comment for some special intentions.

### Minimise Coupling
- Eliminate, minimise, and reduce complexity of necessary relationships.
- By hiding implementation details, coupling is reduced.

**What applied:** Encapsulation in OOP, DI in Spring.

### Inversion of Control
IoC inverts the flow of control as compared to traditional control flow (Don't call us, we'll call you).
- In traditional programming: our custom code makes calls to a library.
- IoC: framework make calls to our custom code.

**What applied:** Spring IoC container with Constructor-Based Dependency Injection for main code and Field-Based Dependency Injection for test code.

### Single Responsibility Principle
Every class should have a single responsibility, and that responsibility should be entirely encapsulated by the class. Responsibility can be defined as a reason to change, so a class or module should have one, and only one, reason to change.

**What applied:** break system into multiple services, each services has only one responsibility. In each services, break into multiple layers, each layers were broken into multiple classes, each class has only one reason to change.

## Design Patterns
- **Singleton Design Pattern**
- **DTO Design Pattern**
- **Facade Design Pattern**
- **Strategy Design Pattern**

## Application default configuration
To make it easier for development process, we still expose these ports on the local machine to send request directly with services or to view actual data in the data stores. 
In production environment, we leverage the infrastructure to make the downstream services become unreachable from the client, we only expose one single point - API Gateway.

| Service               | Port |
|-----------------------| --   |
| api-gateway           | 8080 |
| inventory-service     | 8081 |
| user-service          | 8082 |
| cart-service          | 8083 |

_Note_: Legacy instruction (bypassing auth by injecting a `Username` header) has been removed in this simplified setup; always use Basic Auth (`demo:demo`) unless you extend auth.

## How to run the application
### Setup development workspace (Legacy Section – Superseded)
Use the [Quick Start](#quick-start-two-commands). No MySQL or manual IDE import steps are required beyond cloning. This legacy section is retained only for historical comparison.

### Run a microservice (Optional Advanced Usage)
Normally you use `./run.sh`. For targeted service development you may run one service directly (from that service directory):
```
./mvnw -q -DskipTests package && java -jar target/<service>-1.0.0.jar --server.port=808X
```
Or during iterative coding:
```
./mvnw spring-boot:run
```
Ensure the other dependent services are also running (via another terminal with `./run.sh --force-restart` or individual launches). Frontend static assets are copied only when using `run.sh`; if you run the gateway alone after editing frontend code, execute `npm run build` in `frontend/` then copy `frontend/dist/*` into `api-gateway/src/main/resources/static/` manually or just re-run `./run.sh`.

## Project folder structure and Frameworks, Libraries
### Project folder structure
Based on above design, the project folder structure is organized following:
- api-gateway: API Gateway
- inventory-service: Inventory Service
- external-files: external files
- user-service: User Service
- cart-service: Cart Service

For each microservice, we will follow common 4 layers architecture:
- **Controller**: Handle HTTP request from client, invoke appropriate methods in service layer, return the result to client.
- **Service**: All business logic here. Data related calculations and all.
- **Repository**: all the Database related operations are done here.
- **Entity**: persistent domain object -  table in Databases.

### Frameworks and Libraries
The Frameworks/Libraries used in the project and their purposes:
// (Eureka dependencies removed for simplified setup)
- spring-boot-starter-web : for building REST API.
- spring-boot-starter-test : Starter for testing Spring Boot applications with libraries including JUnit, Hamcrest and Mockito.
- spring-boot-starter-aop : for aspect-oriented programming with Spring AOP and AspectJ. We use this feature for implementing the customer audit feature.
- spring-boot-starter-data-jpa: for using Spring Data JPA with Hibernate.
- spring-boot-starter-validation: for using Java Bean Validation with Hibernate Validator.
- spring-boot-starter-security: for using Spring Security.
- spring-security-test: for the testing Spring Security.
- modelmapper: to make object mapping easy, by automatically determining how one object model maps to another, based on conventions.

## Auth & Security (Demo Implementation)
Current auth is intentionally minimal:
* Basic Auth over HTTPS is recommended (this demo does not provision TLS automatically).
* The user-service exposes a `UserDTO` including a SHA‑256 password hash (demo only). The gateway hashes the presented Basic Auth password with the same algorithm and compares.
* Roles are stored already prefixed (`ROLE_USER`, `ROLE_ADMIN`, etc.). The gateway avoids double-prefixing.
* All mutation endpoints (cart add/remove, listing user details) require authentication; product catalog `GET /api/v1/items` is public.

Security Caveats (Do NOT use as-is in production):
1. Exposing password hashes via service DTO is insecure outside of a controlled demo.
2. No account lockout / rate limiting.
3. No TLS termination provided out of the box.
4. No CSRF tokens (API-only + Basic usage) or refresh token strategy.

Planned (see Roadmap): replace with JWT access/refresh tokens, dedicated auth endpoints, stronger password encoding (e.g., bcrypt with salt), and removal of password hash from outward DTOs.

## Data Persistence & Reset
Each service persists its H2 database under `./data/<service>-db.*`. To reset all data (including carts, users, inventory back to seeded defaults):
```
rm -f data/*-db.*
./run.sh --force-restart
```
Users and inventory will be reseeded automatically; carts will be empty.

## Changelog
### 1.1.1
* Fix: Gateway Basic Auth failure for cart endpoints – user-service `UserDTO` now includes hashed password so gateway can authenticate.
* Docs: Added Auth & Security section, Data Persistence & Reset, Changelog; removed obsolete MySQL requirement & legacy bypass header note; clarified role handling.
* Minor: Troubleshooting entry for historical cart 500 error.

### 1.1.0
* Simplified architecture (removed discovery by default, static routing, deterministic seeding, shared cart context).

## Appendix A – Re‑Enable Service Discovery (Optional)
If you need dynamic service registration (e.g., scaling instances or changing ports), you can restore Eureka:

1. Reintroduce modules/dependencies:
  - Add dependency `spring-cloud-starter-netflix-eureka-client` to each service `pom.xml` (gateway, inventory, user, cart).
  - (Optionally) restore a `discovery-service` module with `spring-cloud-starter-netflix-eureka-server` and an `@EnableEurekaServer` application class.
2. Re-add `@EnableDiscoveryClient` (or `@EnableEurekaClient`) annotations to each service main class (gateway + backends).
3. Add property to each service `application.properties`:
  `spring.application.name=<service-name>`
  `eureka.client.serviceUrl.defaultZone=http://localhost:8084/eureka`
4. Switch gateway routes from static `http://localhost:<port>` URIs back to `lb://<service-name>` in `application.properties`.
5. (Optional) Update `run.sh` to start discovery first and wait for its `/actuator/health` endpoint before launching other services.
6. Remove the static‑routing comment blocks from README.

Rollback is simply removing those dependencies and properties again.

## Version
  1.1.1 (auth DTO fix; security docs; no discovery by default; shared cart context; deterministic seeding refinements)

## License
  Copyright &copy; 2023. All Right Reserved.<br>
  This project is licensed under the [MIT License](LICENSE.txt)

## References
- [Designing a microservices architecture](https://docs.microsoft.com/en-us/azure/architecture/microservices/design/) - *Azure Architecture Center | Microsoft Docs*
- [Cloud design patterns](https://docs.microsoft.com/en-us/azure/architecture/patterns/) - *Azure Architecture Center | Microsoft Docs*
- [The System Design Primer](https://github.com/donnemartin/system-design-primer)

## Contact Details

* Email: visalsrimanga@gmail.com
* Linkedin: Visal Srimanga

---

## Roadmap / Optional Enhancements
These are intentionally deferred to keep the core demo minimal:
1. API Documentation: Add Springdoc OpenAPI (swagger-ui) exposed via gateway.
2. Auth Hardening: Replace Basic Auth with JWT access/refresh tokens; user registration & password hashing improvements (stronger encoder + salting).
3. CI Pipeline: GitHub Actions workflow (build matrix Java 17/21 + frontend build + test + docker image publish).
4. Container Optimization: Multi-stage Dockerfiles with distroless base images and SBOM generation.
5. Observability: Add Spring Boot Actuator metrics + Prometheus scraper config + Grafana dashboard JSON.
6. Caching Layer: Introduce Redis (docker-compose optional service) for product catalog and cart session caching.
7. Resilience: Apply Resilience4j (bulkhead, retry) around cross-service calls (if discovery restored).
8. Frontend Enhancements: Better error boundaries, suspense for product loading, optimistic cart updates.
9. Testing: Add contract tests for gateway, component tests with Testcontainers for JPA.
10. Makefile: Provide cross-platform target aliases (fallback to .PHONY with shell detection).

Open an issue or PR if you’d like any of these prioritized.
