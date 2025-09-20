
# Shopping Cart Web App – Self‑Contained Fullstack Microservices

Fully self‑contained demo shopping cart platform: Spring Boot microservices (API Gateway, Inventory, Cart, User) + React/Vite frontend. Service discovery (Eureka) was intentionally removed to minimize moving parts and guarantee a friction‑free two‑command startup. An appendix explains how to re‑enable discovery if desired.

## Quick Start (Two Commands)

### 0. Prerequisites
Install (and ensure they are on your PATH):
* JDK 17+
* (Optional) Maven 3.8+ (if absent, project Maven Wrappers will be used automatically)
* Node.js 18+ (npm included)
* (Optional) Docker + Docker Compose plugin if you want container mode

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
```

Examples:
```bash
./run.sh --rebuild
./run.sh --force-restart
./run.sh --docker --build   # container mode (optional)
```

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

Seed demo credentials (basic auth for protected endpoints):
```
Username: demo
Password: demo
```

Smoke test after startup:
```bash
bash scripts/smoke.sh
```

## Troubleshooting
| Symptom | Cause | Fix |
|---------|-------|-----|
| `permission denied: ./setup.sh` | Missing execute bit on Unix | `chmod +x setup.sh run.sh` or use `bash setup.sh` |
| `command not found: mvn` | Global Maven not installed | Safe to ignore: scripts fall back to per-service `mvnw` |
| Skipped service (port in use) | Previous instance still running | Use `./run.sh --force-restart` to kill & restart |
| Want a clean rebuild | Incremental jar not refreshed | Run with `--rebuild` |
| Old plugin exit 143 lines | Legacy spring-boot:run noise | Eliminated: jars now launched directly |
| `docker: command not found` when using `--docker` | Docker not installed | Install Docker Desktop / Engine or run without `--docker` |
| Frontend 404 for assets | Gateway started before build finished (rare) | Re-run `./run.sh` or manually `npm run build` then restart gateway |
| Port already in use | Another process occupying required port | Adjust ports in `.env` then rerun setup/run |
| PowerShell script blocked | Execution policy restriction | `Set-ExecutionPolicy -Scope Process RemoteSigned` |
| Tests fail on first run due to missing deps | `npm install` not finished | Re-run `./setup.sh` (ensures dependencies) |

## Features
* Direct static routing via API Gateway (no discovery layer required)
* API Gateway routing + CORS + Basic Auth (username lookup endpoint: `/api/v1/users/username/{username}`)
* Inventory management (seeded products)
* Cart with price snapshots & summary endpoint
* User service with hashed passwords & roles
* React frontend (products + cart view)
* Embedded H2 file DBs (no external database needed)
* Docker & docker-compose optional
* Fallback logic if Docker missing
* Persistent dev data lives in `./data` (root) and is `.gitignore`d (safe to delete for a clean slate)

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

_Note_: for development purpose, we could bypass authentication by adding "Username: ```<your-test-username>```" to HTTP Header when we send request to downstream services.

## How to run the application
### Setup development workspace
The setup development workspace process is simpler than ever with following steps:
1. Install [JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html).
1. Install [Maven](https://maven.apache.org/download.cgi?Preferred=ftp://mirror.reverse.net/pub/apache/).
1. Install MySQL.
1. Clone this project to your local machine.
1. Open the pom.xml file and open as a project using Intellij IDEA.

That's all.

### Run a microservice
You can run Spring Boot microservice in different ways, but first make sure you are in the root directory of the microservice you want to run:
- Run jar file (of course you need to build it first): ```mvn install && java -jar target/<service-name>-1.0.0.jar```
- Run with Spring Boot: ```mvn spring-boot:run```

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
  1.0.0 (simplified – no discovery; jar-based launcher)

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
