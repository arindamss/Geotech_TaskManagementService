# Task Manager Service

A REST service for managing tasks that can depend on one another
(e.g. *Task B cannot start until Task A is DONE*).

**Stack:** Java 21 · Spring Boot 3.5 · Spring Data JPA/Hibernate · PostgreSQL · Maven · Docker

---

## 1. Running the project

### Option A: Docker Compose (recommended)

Requires Docker only.

```bash
docker-compose up --build
```

The API is available at `http://localhost:8080` and Swagger UI at `http://localhost:8080/swagger-ui.html`.
The app waits for PostgreSQL to be healthy before starting.

```bash
docker-compose down       # stop
docker-compose down -v    # stop and wipe the database volume
```

### Option B: Locally

Requires JDK 21+, Maven 3.9+ and Docker (for the database only).

```bash
docker-compose up -d db     # start PostgreSQL on localhost:5432
mvn spring-boot:run         # start the app on :8080
```

Run the tests (no database needed, they are unit tests):

```bash
mvn test
```

Database settings live in `src/main/resources/application.yml` and can be overridden with
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.

---

## 2. API

Base path: `/api/tasks`. Interactive docs: `/swagger-ui.html` (OpenAPI JSON at `/v3/api-docs`).

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `POST` | `/api/tasks` | Create a task | `201` |
| `GET` | `/api/tasks?status=` | List tasks, optionally filtered by status | `200` |
| `GET` | `/api/tasks/{id}` | Task details | `200` |
| `PATCH` | `/api/tasks/{id}/status` | Update task status | `200` |
| `POST` | `/api/tasks/{id}/dependencies` | Make the task depend on another task | `201` |
| `DELETE` | `/api/tasks/{id}/dependencies/{dependsOnId}` | Remove a dependency | `204` |
| `GET` | `/api/tasks/execution-order` | All tasks in a valid execution order | `200` |

### Task representation

```json
{
  "id": 3,
  "title": "Deploy",
  "description": "Deploy to production",
  "status": "TODO",
  "createdAt": "2026-10-02T10:15:30.123Z",
  "blocked": true,
  "dependencies": [2]
}
```

`blocked` is **derived**, not stored: it is `true` when the task has at least one dependency that is not `DONE`.

### Request bodies

```jsonc
// POST /api/tasks
{ "title": "Build feature", "description": "optional" }

// PATCH /api/tasks/{id}/status       (TODO | IN_PROGRESS | DONE)
{ "status": "IN_PROGRESS" }

// POST /api/tasks/{id}/dependencies  (this task depends on task 2)
{ "dependsOnId": 2 }
```

### Error responses

Errors use [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Task 2 cannot move to IN_PROGRESS: blocked by unfinished tasks [1]"
}
```

| Situation | Status |
|-----------|--------|
| Task not found | `404` |
| Invalid request body / unknown status value / validation failure | `400` (validation includes an `errors` map per field) |
| Invalid dependency (self-dependency, unknown dependency task, removing a dependency that does not exist) | `400` |
| Circular dependency attempt | `409` |
| Starting/completing a blocked task | `409` |

### Example walkthrough

```bash
curl -X POST localhost:8080/api/tasks -H 'Content-Type: application/json' -d '{"title":"Design"}'    # id 1
curl -X POST localhost:8080/api/tasks -H 'Content-Type: application/json' -d '{"title":"Build"}'     # id 2
curl -X POST localhost:8080/api/tasks -H 'Content-Type: application/json' -d '{"title":"Release"}'   # id 3

# Build depends on Design; Release depends on Build
curl -X POST localhost:8080/api/tasks/2/dependencies -H 'Content-Type: application/json' -d '{"dependsOnId":1}'
curl -X POST localhost:8080/api/tasks/3/dependencies -H 'Content-Type: application/json' -d '{"dependsOnId":2}'

# Rejected: would create a cycle (409)
curl -X POST localhost:8080/api/tasks/1/dependencies -H 'Content-Type: application/json' -d '{"dependsOnId":3}'

# Rejected: Design is not DONE yet (409)
curl -X PATCH localhost:8080/api/tasks/2/status -H 'Content-Type: application/json' -d '{"status":"IN_PROGRESS"}'

# Finish Design, then Build can start
curl -X PATCH localhost:8080/api/tasks/1/status -H 'Content-Type: application/json' -d '{"status":"IN_PROGRESS"}'
curl -X PATCH localhost:8080/api/tasks/1/status -H 'Content-Type: application/json' -d '{"status":"DONE"}'
curl -X PATCH localhost:8080/api/tasks/2/status -H 'Content-Type: application/json' -d '{"status":"IN_PROGRESS"}'

# Valid execution order: [1, 2, 3]
curl localhost:8080/api/tasks/execution-order
```

---

## 3. Architecture

### Layers

```
com.example.taskmanager
├── controller   HTTP only: routing, validation trigger, status codes
├── service      Business rules, transactions, graph algorithms
├── repository   Spring Data JPA interfaces
├── domain       JPA entity (Task) and enum (TaskStatus)
├── dto          Request/response records (API contract, decoupled from entities)
└── exception    Domain exceptions + global handler mapping them to HTTP
```

### Components and request flow

```
Client
  │  HTTP/JSON
  ▼
TaskController ──► @Valid on DTO ──(invalid)──► GlobalExceptionHandler ─► 400
  │
  ▼
TaskService  (@Transactional)
  │   • load entities           ──(missing)──► TaskNotFoundException ─────► 404
  │   • enforce business rules  ──(violated)─► Circular / InvalidStatus ──► 409
  │   • mutate entity state (dirty checking flushes on commit)
  ▼
TaskRepository ──► Hibernate ──► PostgreSQL
                                   ├── tasks
                                   └── task_dependencies (task_id, depends_on_id)
  │
  ▼
TaskResponse DTO ──► JSON
```

Example: `PATCH /api/tasks/2/status {"status":"DONE"}`
1. Controller validates the body and calls `TaskService.updateStatus`.
2. Service loads the task (dependencies are fetched in the same transaction).
3. If the target status is not `TODO` and any dependency is not `DONE`, it throws `InvalidStatusTransitionException` (409), listing the blocking task ids.
4. Otherwise the status is set; Hibernate persists it on commit; a `TaskResponse` is returned.

### Data model

`tasks(id, title, description, status, created_at)` and a join table
`task_dependencies(task_id, depends_on_id)` modelling a directed graph: the edge *task → depends_on* means "task requires depends_on".

---

## 4. Design decisions and assumptions

**Decisions**

- **Dependencies as a self-referencing `@ManyToMany`.** The graph is small and the join table carries no extra attributes, so a join entity would be needless ceremony.
- **`blocked` is computed, not persisted.** Storing it would require updating every dependent whenever a task changes status and risks stale data. Computing it on read keeps a single source of truth.
- **Cycle prevention at write time.** Before adding *A → B* the service runs an iterative DFS from B following its dependencies. If it reaches A, the edge would close a cycle and is rejected with `409`. The graph is therefore always a DAG, which is also what makes the execution order always possible. DFS is iterative to avoid stack overflow on long chains.
- **Execution order via Kahn's algorithm** (`ExecutionOrderResolver`). Linear in tasks + edges, and ties are broken by id so the output is deterministic. It lives in its own small class so it is easy to test.
- **Business rules live in the service/domain, not the controller.** The entity exposes `blockingTasks()` / `isBlocked()`; the service decides what to do with them.
- **DTOs (Java records) at the API boundary.** Entities are never serialised, so the API contract can evolve independently of the schema and lazy-loading issues cannot leak into JSON.
- **Consistent errors with RFC 7807 `ProblemDetail`** through a single `@RestControllerAdvice`.
- **N+1 avoidance.** List queries use `@EntityGraph` to fetch dependencies in one query. `spring.jpa.open-in-view` is disabled so DB access stays inside the service transaction.
- **Idempotent dependency creation.** Adding a dependency that already exists succeeds without change.

**Assumptions**

- A task may be set back to `TODO` at any time. Rules 1 and 2 only restrict moving to `IN_PROGRESS` or `DONE`. Other transitions (e.g. `DONE → IN_PROGRESS`) are not restricted beyond those rules, since the spec does not define a state machine.
- A new dependency can be added to a task regardless of its current status. If the task is already `IN_PROGRESS` and the new dependency is not done, it simply shows as `blocked: true`.
- A dependency task being moved back from `DONE` does not automatically change the status of dependents.
- `/execution-order` returns all tasks (including `DONE` ones) so that clients see the full picture; each item has its `status`.
- No authentication/authorisation: out of scope for this assignment.

**Production follow-ups (deliberately left out to keep this lean)**

- Replace `ddl-auto: update` with Flyway/Liquibase migrations.
- Add integration tests (Testcontainers + PostgreSQL) and pagination on the list endpoint.
- Add optimistic locking (`@Version`) to protect against concurrent dependency edits that could race past cycle detection.
