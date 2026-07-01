# DeployForge — Professional SaaS Deployment Platform

DeployForge is a production-quality SaaS platform designed for developers to register, create projects, validate public Git repository reachability E2E, trigger asynchronous builds, monitor live logs, and access live deployment versions.

This codebase follows a **Modular Monolith** backend architecture (Java 21 + Spring Boot 3) coupled with a modern React SPA client (Vite + TypeScript + Tailwind CSS v4).

---

## 1. System Architecture

DeployForge decouples transactional metadata, queue brokerage, and append-heavy logging into three specialized datastores:

- **PostgreSQL**: System of record managing Users, Projects, Deployments, and DeploymentVersions (using Flyway SQL schema migrations and UUID everywhere).
- **Redis**: Asynchronous job queue using Redis list primitives under a unified `DeploymentQueue` interface.
- **MongoDB**: Standalone high-throughput logging engine storing build, container setup, and execution log streams.

For full sequence flowcharts, refer to the approved [architecture.md](file:///.gemini/antigravity-ide/brain/c753cc39-3720-49ba-8556-8078abbfaa3e/architecture.md) artifact.

---

## 2. Technology Stack

### Backend
- **Java 21** & **Spring Boot 3** (Spring Security, Data JPA, Redis, MongoDB, Validation)
- **Flyway** (PostgreSQL schema migrations)
- **JGit** (Secure remote Git repository check)
- **Springdoc OpenAPI** (Automatic API Documentation and Swagger UI)
- **JJWT** (Access control JWT verification)

### Frontend
- **React 19**, **Vite 8**, **TypeScript**
- **Tailwind CSS v4** (Utility styles)
- **React Router** & **TanStack React Query**

---

## 3. Quick Start (Dockerized Orchestration)

To spin up the entire containerized platform (Databases, Backend Service, Frontend Nginx):

```bash
docker compose up --build -d
```

### Access Points
- **Frontend App**: [http://localhost:3000](http://localhost:3000)
- **API Server Gateway**: [http://localhost:8080](http://localhost:8080)
- **Swagger API Docs**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## 4. Local Development Guide

If you prefer to run services individually for debugging/testing:

### Step 1: Start Databases
```bash
docker compose up postgres redis mongodb -d
```
*Note: If port 27017 is in use on your host, the Docker MongoDB will map to port 27018. The Spring Boot backend connects to your host's local MongoDB instance on port 27017.*

### Step 2: Build & Start Backend
```bash
cd backend
mvn clean package
java -jar target/backend-1.0.0.jar
```

### Step 3: Start React Client
```bash
cd frontend
npm install
npm run dev
```

---

## 5. API Reference Summary

The gateway REST APIs expose the following contract (fully documented under Swagger UI):

### Authentication (`/api/auth`)
- `POST /api/auth/register` — Create a new developer account.
- `POST /api/auth/login` — Log in and retrieve Bearer JWT.

### Projects (`/api/projects`) — *Requires Authorization*
- `POST /api/projects` — Create a project and ping its Git repository URL.
- `GET /api/projects` — List owned projects.
- `GET /api/projects/{id}` — Fetch project details.
- `PUT /api/projects/{id}` — Update project description/repository.
- `DELETE /api/projects/{id}` — Delete project and historical deployments.

### Deployments (`/api/deployments`) — *Requires Authorization*
- `POST /api/deployments` — Enqueue a new async deployment execution.
- `GET /api/deployments/{id}` — Get deployment run details.
- `GET /api/deployments/project/{projectId}` — List all historical runs for a project.

### Logging (`/api/logs`) — *Requires Authorization*
- `GET /api/logs/{deploymentId}` — Fetch chronological logs from MongoDB.

---

## 6. End-to-End Verification Script

A verification shell script is provided at the root directory to execute all REST endpoints using custom JWT parameters:

```bash
./verify_api.sh
```
This script tests:
1. Registering/Logging in.
2. Invalid git URL format blocks.
3. Unreachable repository blocks.
4. Successful project creation.
5. Async deployment status polling and console logging output.
