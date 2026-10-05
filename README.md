# Task Manager Service

A REST service for managing tasks that can depend on one another
(e.g. _Task B cannot start until Task A is DONE_).

**Stack:** Java 21 · Spring Boot 4.1.1 · Spring Data JPA/Hibernate · PostgreSQL · Maven · Docker

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

Database settings live in `src/main/resources/application-docker.yml` and can be overridden with
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.

---

## 2. API

Base path: `/api/tasks`. All task ids are UUIDs.
Interactive docs: `/swagger-ui.html` (OpenAPI JSON at `/v3/api-docs`).

| Method   | Path                                         | Description                          | Success |
| -------- | -------------------------------------------- | ------------------------------------ | ------- |
| `POST`   | `/api/tasks`                                 | Create a task                        | `201`   |
| `GET`    | `/api/tasks`                                 | List all tasks                       | `200`   |
| `GET`    | `/api/tasks/{id}`                            | Get task details                     | `200`   |
| `PATCH`  | `/api/tasks/{id}/status`                     | Update task status                   | `200`   |
| `POST`   | `/api/tasks/{id}/dependencies`               | Make the task depend on another task | `200`   |
| `GET`    | `/api/tasks/{id}/dependencies`               | List the task's dependencies         | `200`   |
| `DELETE` | `/api/tasks/{id}/dependencies/{dependsOnId}` | Remove a dependency                  | `204`   |
| `GET`    | `/api/tasks/execution-order`                 | Valid execution order of all tasks   | `200`   |

### Request bodies

```jsonc
// POST /api/tasks
{ "title": "Build feature", "description": "optional" }

// PATCH /api/tasks/{id}/status      (TODO | IN_PROGRESS | DONE)
{ "status": "IN_PROGRESS" }

// POST /api/tasks/{id}/dependencies (the task in the path depends on dependencyId)
{ "dependencyId": "3f2b8c1e-7a4d-4b9e-9c55-0d1f6a2e8b10" }
```

### Responses

**Task** (`POST /api/tasks`, `GET /api/tasks`, `GET /api/tasks/{id}`, status and dependency updates):

```json
{
  "id": "3f2b8c1e-7a4d-4b9e-9c55-0d1f6a2e8b10",
  "title": "Deploy",
  "description": "Deploy to production",
  "status": "TODO",
  "createdAt": "2026-10-02T10:15:30.123Z",
  "dependencies": ["a1c4e7f2-...-..."]
}
```

**Execution order** (`GET /api/tasks/execution-order`): task ids, each task listed after all tasks it depends on:

```json
{
  "executionOrder": ["a1c4e7f2-...-...", "3f2b8c1e-...-..."]
}
```

### Business rules enforced by the API

1. A task cannot move to `IN_PROGRESS` or `DONE` while any of its dependencies is not `DONE`.
2. A task cannot depend on itself, or on a task it already depends on.
3. A dependency that would create a cycle (direct or indirect) is rejected.
4. A task is _blocked_ when at least one dependency is not `DONE`.

### Error responses

Errors are returned as `application/problem+json` (RFC 7807):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Task <id> cannot move to IN_PROGRESS: blocked by unfinished tasks [<id>]"
}
```

| Situation                                                                                             | Status |
| ----------------------------------------------------------------------------------------------------- | ------ |
| Task (or dependency task) not found                                                                   | `404`  |
| Validation failure, malformed body, unknown status value, malformed UUID                              | `400`  |
| Self-dependency, duplicate dependency, circular dependency, removing a dependency that does not exist | `400`  |
| Moving a blocked task to `IN_PROGRESS` / `DONE`                                                       | `409`  |

### Example walkthrough

```bash
# Create tasks (note the returned ids)
curl -X POST localhost:8080/api/tasks -H 'Content-Type: application/json' -d '{"title":"Design"}'
curl -X POST localhost:8080/api/tasks -H 'Content-Type: application/json' -d '{"title":"Build"}'
curl -X POST localhost:8080/api/tasks -H 'Content-Type: application/json' -d '{"title":"Release"}'

# Build depends on Design; Release depends on Build
curl -X POST localhost:8080/api/tasks/$BUILD/dependencies   -H 'Content-Type: application/json' -d "{\"dependencyId\":\"$DESIGN\"}"
curl -X POST localhost:8080/api/tasks/$RELEASE/dependencies -H 'Content-Type: application/json' -d "{\"dependencyId\":\"$BUILD\"}"

# Rejected (400): Design -> Release would close a cycle
curl -X POST localhost:8080/api/tasks/$DESIGN/dependencies -H 'Content-Type: application/json' -d "{\"dependencyId\":\"$RELEASE\"}"

# Rejected (409): Design is not DONE yet
curl -X PATCH localhost:8080/api/tasks/$BUILD/status -H 'Content-Type: application/json' -d '{"status":"IN_PROGRESS"}'

# Finish Design, then Build can start
curl -X PATCH localhost:8080/api/tasks/$DESIGN/status -H 'Content-Type: application/json' -d '{"status":"IN_PROGRESS"}'
curl -X PATCH localhost:8080/api/tasks/$DESIGN/status -H 'Content-Type: application/json' -d '{"status":"DONE"}'
curl -X PATCH localhost:8080/api/tasks/$BUILD/status  -H 'Content-Type: application/json' -d '{"status":"IN_PROGRESS"}'

# Execution order: Design, Build, Release
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
  │   • enforce business rules  ──(violated)─► InvalidStatus ──► 409
  │   • mutate entity state (dirty checking flushes on commit)
  ▼
TaskRepository ──► Hibernate ──► PostgreSQL
                                   ├── tasks
                                   └── task_dependencies (task_id, dependency_id)
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
`task_dependencies(task_id, dependency_id)` modelling a directed graph: the edge _task → depends_on_ means "task requires depends_on".

---

## 4. Design decisions and assumptions

**Decisions**

- **Dependencies as a self-referencing `@ManyToMany`.** The graph is small and the join table carries no extra attributes, so a join entity would be needless ceremony.
- **`blocked` is computed, not persisted.** Storing it would require updating every dependent whenever a task changes status and risks stale data. Computing it on read keeps a single source of truth.
- **Cycle prevention at write time.** Before adding _A → B_ the service runs an iterative DFS from B following its dependencies. If it reaches A, the edge would close a cycle and is rejected with `409`. The graph is therefore always a DAG, which is also what makes the execution order always possible. DFS is iterative to avoid stack overflow on long chains.
- **Execution order via Kahn's algorithm** (`ExecutionOrderResolver`). Linear in tasks + edges, and ties are broken by id so the output is deterministic. It lives in its own small class so it is easy to test.
- **Business rules live in the service/domain, not the controller.** The entity exposes `blockingTasks()` / `isBlocked()`; the service decides what to do with them.
- **DTOs (Java records) at the API boundary.** Entities are never serialised, so the API contract can evolve independently of the schema and lazy-loading issues cannot leak into JSON.
- **Consistent errors with RFC 7807 `ProblemDetail`** through a single `@RestControllerAdvice`.
- **N+1 avoidance.** Tasks are loaded with their dependencies in a single `JOIN FETCH` query, and `open-in-view` is disabled so lazy loading stays inside service transactions.
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
