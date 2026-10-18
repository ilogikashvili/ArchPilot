# ArchMain — Architecture Review & Modeling Platform API Reference

ArchMain is the primary backend service for the ArchPilot platform. It provides APIs for user authentication, project workspace management, diagram architecture modeling, automated architectural quality reviews, finding decision tracking, team collaboration, and architecture import/export.

---

## Architecture & Design Patterns

The service is built on Spring Boot 3+ (Java 17) and strictly adheres to the following principles:
- **Layered Architecture**: `Controller` &rarr; `Service` &rarr; `Repository`. Controllers never touch repositories; entities never leave the service layer.
- **DTO + Mapper Pattern**: Entities and database schemas are decoupled from public HTTP payloads via dedicated records and Spring component mappers.
- **Access Control Facade**: All project-level authorization is checked via `ProjectAccessService.require(projectId, userId, Permission)`.
- **Strategy Pattern & Registries**:
  - `ArchitectureValidator` delegates to individual `ValidationRule` implementations.
  - `RuleBasedReviewEngine` delegates to individual `ReviewRule` implementations.
  - `ExporterRegistry` looks up `ArchitectureExporter` and `ArchitectureImporter` implementations by format (e.g. `json`).
- **State Pattern**: `ReviewStateMachine` governs valid review lifecycle transitions (`QUEUED` &rarr; `RUNNING` &rarr; `COMPLETED` / `FAILED` / `CANCELLED`).
- **Optimistic Concurrency**: Architecture document persistence enforces revision checking (`expectedRevision`) to prevent overwrite conflicts.
- **Proposal Patch Pattern**: `RecommendationApplier` returns architectural preview patches rather than directly mutating diagrams without user review.

---

## Authentication & Headers

- All protected endpoints require a JWT bearer token in the `Authorization` header:
  ```http
  Authorization: Bearer <access_token>
  ```
- All endpoints are versioned under prefix `/api/v1`.

---

## Complete API Reference

### 1. Authentication (`/api/v1/auth`)

#### `POST /api/v1/auth/register`
- **Description**: Creates a new user account and returns JWT credentials.
- **Auth**: Public
- **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "StrongPassword123!",
    "displayName": "Jane Doe"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1Ni...",
    "refreshToken": "eyJhbGciOiJIUzI1Ni...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```
- **Implementation**:
  - Validates email uniqueness in `UserRepository` (throws `ConflictException` if taken).
  - Encrypts password using `BCryptPasswordEncoder`.
  - Persists `User` entity.
  - Generates signed JWT access & refresh tokens via `JwtTokenProvider`.

---

#### `POST /api/v1/auth/login`
- **Description**: Authenticates user credentials and starts a session.
- **Auth**: Public
- **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "StrongPassword123!"
  }
  ```
- **Response**: `200 OK` (`AuthTokenResponse`)
- **Implementation**:
  - Finds user by email in `UserRepository`.
  - Validates raw password against BCrypt hash (throws `BadCredentialsException` on mismatch).
  - Checks if user account is active.
  - Emits new JWT access and refresh token pair.

---

#### `POST /api/v1/auth/logout`
- **Description**: Ends the current session.
- **Auth**: Authenticated (`Bearer <token>`)
- **Response**: `204 No Content`
- **Implementation**:
  - Client discards stored tokens (stateless JWT). Server-side token revocation hook is ready for blacklist expansion.

---

#### `GET /api/v1/auth/me`
- **Description**: Returns the profile and identity of the currently authenticated user.
- **Auth**: Authenticated (`Bearer <token>`)
- **Response**: `200 OK`
  ```json
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "user@example.com",
    "displayName": "Jane Doe",
    "avatarUrl": null,
    "emailVerified": false,
    "createdAt": "2026-10-17T10:00:00Z"
  }
  ```
- **Implementation**:
  - `CurrentUserArgumentResolver` injects `AuthenticatedUser` extracted from JWT claims.
  - `UserService.getById()` queries `UserRepository` and maps the entity to `CurrentUserResponse`.

---

#### `POST /api/v1/auth/refresh`
- **Description**: Refreshes an expiring access credential using a valid refresh token.
- **Auth**: Public
- **Request Body**:
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1Ni..."
  }
  ```
- **Response**: `200 OK` (`AuthTokenResponse`)
- **Implementation**:
  - `AuthService.refresh()` validates refresh token signature and expiration via `JwtTokenProvider`.
  - Verifies user exists and generates a fresh token pair.

---

#### `POST /api/v1/auth/forgot-password`
- **Description**: Requests password recovery.
- **Auth**: Public
- **Request Body**:
  ```json
  {
    "email": "user@example.com"
  }
  ```
- **Response**: `202 Accepted`
- **Implementation**:
  - Looks up user by email; if found, generates a signed 15-minute `password_reset` JWT token.
  - Prepares notification / event dispatch to deliver recovery link to user's email.

---

#### `POST /api/v1/auth/reset-password`
- **Description**: Resets password using a recovery token.
- **Auth**: Public
- **Request Body**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1Ni...",
    "newPassword": "NewStrongPassword123!"
  }
  ```
- **Response**: `204 No Content`
- **Implementation**:
  - Validates recovery token signature, expiration, and `type == "password_reset"`.
  - Retrieves user by ID from token payload.
  - Hashes new password with BCrypt and updates user record.

---

#### `POST /api/v1/auth/verify-email`
- **Description**: Verifies account email address using a verification token.
- **Auth**: Public
- **Request Body**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1Ni..."
  }
  ```
- **Response**: `204 No Content`
- **Implementation**:
  - Validates verification token signature, expiration, and `type == "email_verification"`.
  - Sets `emailVerified = true` on the user entity and persists.

---

#### `POST /api/v1/auth/resend-verification`
- **Description**: Resends verification email token.
- **Auth**: Public
- **Request Body**:
  ```json
  {
    "email": "user@example.com"
  }
  ```
- **Response**: `202 Accepted`
- **Implementation**:
  - Finds unverified user and generates a 24-hour verification token.

---

### 2. User Profile Management (`/api/v1/users/me`)

#### `PATCH /api/v1/users/me`
- **Description**: Updates profile details (display name).
- **Auth**: Authenticated (`Bearer <token>`)
- **Request Body**:
  ```json
  {
    "displayName": "Jane D."
  }
  ```
- **Response**: `200 OK` (`UserResponse`)
- **Implementation**:
  - `UserService.updateProfile()` updates fields on `User` entity and returns mapped DTO.

---

#### `PATCH /api/v1/users/me/password`
- **Description**: Changes the current user's password.
- **Auth**: Authenticated (`Bearer <token>`)
- **Request Body**:
  ```json
  {
    "currentPassword": "OldPassword123!",
    "newPassword": "NewStrongPassword123!"
  }
  ```
- **Response**: `204 No Content`
- **Implementation**:
  - Verifies `currentPassword` matches stored BCrypt hash.
  - Updates entity with new BCrypt encoded hash.

---

#### `DELETE /api/v1/users/me`
- **Description**: Requests account deletion.
- **Auth**: Authenticated (`Bearer <token>`)
- **Response**: `204 No Content`
- **Implementation**:
  - Deletes or deactivates the user record via `UserService.requestDeletion()`.

---

### 3. Projects (`/api/v1/projects`)

#### `POST /api/v1/projects`
- **Description**: Creates a new architecture project.
- **Auth**: Authenticated (`Bearer <token>`)
- **Request Body**:
  ```json
  {
    "name": "E-Commerce Microservices",
    "description": "Target architecture for checkout & catalog"
  }
  ```
- **Response**: `201 Created` (`ProjectResponse`)
- **Implementation**:
  - Saves new `Project` entity with `ownerId = currentUser.id()`.
  - Automatically records the creator in `ProjectMemberRepository` with `ProjectRole.OWNER`.
  - Calls `ArchitectureService.initializeForProject(projectId)` to create an empty `ArchitectureDocumentEntity` ready for editing.

---

#### `GET /api/v1/projects`
- **Description**: Lists all projects accessible to the caller.
- **Auth**: Authenticated (`Bearer <token>`)
- **Response**: `200 OK` (`List<ProjectResponse>`)
- **Implementation**:
  - Calls `ProjectRepository.findAccessibleByUserId(userId)` which joins memberships to return all projects where the user is a member.

---

#### `GET /api/v1/projects/{projectId}`
- **Description**: Retrieves details of a specific project.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK` (`ProjectResponse`)
- **Implementation**:
  - `ProjectAccessService.require(projectId, userId, Permission.READ)`.
  - Queries `ProjectRepository.findById(projectId)`.

---

#### `PATCH /api/v1/projects/{projectId}`
- **Description**: Updates project metadata (name, description).
- **Auth**: Authenticated & `Permission.EDIT`
- **Request Body**:
  ```json
  {
    "name": "Updated Project Name",
    "description": "Updated description"
  }
  ```
- **Response**: `200 OK` (`ProjectResponse`)
- **Implementation**:
  - `ProjectAccessService.require(projectId, userId, Permission.EDIT)`.
  - Mutates updated fields on `Project` entity and saves.

---

#### `DELETE /api/v1/projects/{projectId}`
- **Description**: Deletes a project.
- **Auth**: Authenticated & `Permission.DELETE`
- **Response**: `204 No Content`
- **Implementation**:
  - `ProjectAccessService.require(projectId, userId, Permission.DELETE)`.
  - Deletes project from database.

---

#### `GET /api/v1/projects/{projectId}/summary`
- **Description**: Returns project summary and diagram metrics.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK`
  ```json
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "E-Commerce Microservices",
    "description": "Target architecture",
    "ownerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "archived": false,
    "nodeCount": 12,
    "edgeCount": 18,
    "memberCount": 4,
    "updatedAt": "2026-10-17T11:00:00Z"
  }
  ```
- **Implementation**:
  - Verifies read permission, reads diagram model to compute node and edge counts, queries member counts from `ProjectAccessService`.

---

#### `POST /api/v1/projects/{projectId}/duplicate`
- **Description**: Clones a project along with its architecture diagram.
- **Auth**: Authenticated & `Permission.READ`
- **Request Body** (optional):
  ```json
  {
    "name": "E-Commerce Microservices (v2)",
    "description": "Clone for redesign"
  }
  ```
- **Response**: `201 Created` (`ProjectResponse`)
- **Implementation**:
  - Reads existing project and architecture document.
  - Creates new project with caller as `OWNER`.
  - Copies and saves complete architecture document into new project.

---

#### `POST /api/v1/projects/{projectId}/archive` & `POST /api/v1/projects/{projectId}/restore`
- **Description**: Soft-archives or restores a project.
- **Auth**: Authenticated & `Permission.EDIT`
- **Response**: `200 OK` (`ProjectResponse`)
- **Implementation**:
  - Sets `archived = true` or `false` on `Project` entity and persists.

---

### 4. Architecture Modeling & Transfer (`/api/v1/projects/{projectId}/architecture`)

#### `GET /api/v1/projects/{projectId}/architecture`
- **Description**: Fetches the complete architecture diagram document for the project.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK` (`ArchitectureResponse`)
  ```json
  {
    "projectId": "123e4567-e89b-12d3-a456-426614174000",
    "document": {
      "schemaVersion": "1.0",
      "nodes": [
        {
          "id": "node-1",
          "type": "service",
          "label": "Order Service",
          "description": "Handles orders",
          "x": 100.0,
          "y": 150.0,
          "properties": {}
        },
        {
          "id": "node-2",
          "type": "database",
          "label": "Orders DB",
          "description": "PostgreSQL instance",
          "x": 300.0,
          "y": 150.0,
          "properties": {}
        }
      ],
      "edges": [
        {
          "id": "edge-1",
          "type": "sync",
          "sourceId": "node-1",
          "targetId": "node-2",
          "label": "JDBC",
          "properties": {}
        }
      ],
      "viewport": { "x": 0.0, "y": 0.0, "zoom": 1.0 },
      "metadata": {}
    },
    "revision": 3,
    "updatedAt": "2026-10-17T11:30:00Z"
  }
  ```
- **Implementation**:
  - `ProjectAccessService.require(projectId, userId, Permission.READ)`.
  - Retrieves `ArchitectureDocumentEntity` and maps JSON to typed model.

---

#### `PUT /api/v1/projects/{projectId}/architecture`
- **Description**: Saves the entire architecture document with optimistic locking.
- **Auth**: Authenticated & `Permission.EDIT`
- **Request Body**:
  ```json
  {
    "expectedRevision": 3,
    "document": { ... }
  }
  ```
- **Response**: `200 OK` (`ArchitectureResponse`)
- **Error Responses**:
  - `400 Bad Request`: If document fails validation rules (schemaVersion, unique node/edge IDs, dangling edges, size limits).
  - `409 Conflict`: If `expectedRevision != entity.revision` (concurrency clash from another user).
- **Implementation**:
  - `ProjectAccessService.require(projectId, userId, Permission.EDIT)`.
  - Runs `ArchitectureValidator.validate(document)` against all 5 rules.
  - Verifies optimistic revision.
  - Serializes document to JSON, saves entity, increments revision.

---

#### `POST /api/v1/projects/{projectId}/architecture/validate`
- **Description**: Validates a proposed architecture document in-memory without saving it.
- **Auth**: Authenticated
- **Request Body**: `ArchitectureDocument`
- **Response**: `200 OK` (`ValidationResponse`)
  ```json
  {
    "valid": false,
    "errors": [
      {
        "code": "DANGLING_EDGE_REFERENCE",
        "message": "Edge 'edge-2' references missing target node 'node-99'"
      }
    ]
  }
  ```

---

#### `GET /api/v1/projects/{projectId}/architecture/export?format=json`
- **Description**: Exports the architecture diagram to an attachment file.
- **Auth**: Authenticated & `Permission.READ`
- **Query Params**: `format` (`json` default, extensible for `svg`, `png`, `pdf`)
- **Response**: `200 OK` with `Content-Disposition: attachment; filename="architecture-{projectId}.json"`
- **Implementation**:
  - Looks up exporter from `ExporterRegistry`.
  - Serializes document into formatted binary/text payload.

---

#### `POST /api/v1/projects/{projectId}/architecture/import?format=json`
- **Description**: Imports and overwrites the project architecture from an uploaded file payload.
- **Auth**: Authenticated & `Permission.EDIT`
- **Request Body**: Binary/raw bytes of diagram file (e.g. JSON)
- **Response**: `200 OK` (`ArchitectureResponse`)
- **Implementation**:
  - Uses `ArchitectureImporter` to parse the file into `ArchitectureDocument`.
  - Validates structure via `ArchitectureValidator`.
  - Saves new revision in database.

---

#### `POST /api/v1/projects/{projectId}/architecture/import/validate?format=json`
- **Description**: Validates an uploaded import payload without saving or overwriting.
- **Auth**: Authenticated
- **Request Body**: Binary/raw bytes of diagram file
- **Response**: `200 OK` (`ValidationResponse`)

---

### 5. Automated Architecture Reviews (`/api/v1/projects/{projectId}/reviews`)

#### `POST /api/v1/projects/{projectId}/reviews`
- **Description**: Triggers an automated architecture review against the project's current diagram.
- **Auth**: Authenticated & `Permission.REVIEW`
- **Request Body** (optional):
  ```json
  {
    "engineType": "RULE_BASED"
  }
  ```
- **Response**: `201 Created` (`ReviewResponse`)
- **Implementation**:
  - `ProjectAccessService.require(projectId, userId, Permission.REVIEW)`.
  - Loads diagram via `ArchitectureService.getDocument()`.
  - Creates `AiReview` entity in `QUEUED` state.
  - Transitions `QUEUED` &rarr; `RUNNING`.
  - Executes `RuleBasedReviewEngine`:
    - `SingleDatabaseBottleneckRule`: Detects database nodes with &ge; 3 incoming service connections.
    - `MissingCacheRule`: Detects direct database querying without a caching layer (Redis, Memcached).
  - Persists findings in `ReviewFindingRepository`.
  - Transitions `RUNNING` &rarr; `COMPLETED`, sets finding count, returns review.

---

#### `GET /api/v1/projects/{projectId}/reviews`
- **Description**: Lists all historical review runs for the project.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK` (`List<ReviewSummary>`)

---

#### `GET /api/v1/projects/{projectId}/reviews/{reviewId}`
- **Description**: Retrieves review status, execution timing, and summary.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK` (`ReviewResponse`)

---

#### `GET /api/v1/projects/{projectId}/reviews/{reviewId}/findings`
- **Description**: Lists all findings discovered during a specific review execution.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK` (`List<FindingResponse>`)

---

#### `GET /api/v1/projects/{projectId}/reviews/{reviewId}/findings/{findingId}`
- **Description**: Fetches a single finding by ID.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK` (`FindingResponse`)

---

#### `POST /api/v1/projects/{projectId}/reviews/{reviewId}/cancel`
- **Description**: Cancels an in-progress review run.
- **Auth**: Authenticated & `Permission.REVIEW`
- **Response**: `200 OK` (`ReviewResponse`)

---

#### `POST /api/v1/projects/{projectId}/reviews/compare`
- **Description**: Compares two review runs of a project to determine new, resolved, and unchanged findings.
- **Auth**: Authenticated & `Permission.READ`
- **Request Body**:
  ```json
  {
    "baseReviewId": "987fcdeb-51a2-43d7-9012-3456789abcde",
    "targetReviewId": "456abcde-1234-5678-90ab-cdef12345678"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "baseReviewId": "987fcdeb-51a2-43d7-9012-3456789abcde",
    "targetReviewId": "456abcde-1234-5678-90ab-cdef12345678",
    "newFindingsCount": 1,
    "resolvedFindingsCount": 1,
    "unchangedFindingsCount": 2,
    "newFindings": [...],
    "resolvedFindings": [...],
    "unchangedFindings": [...]
  }
  ```
- **Implementation**:
  - Compares finding signatures (`category + title + affectedElementId`) between the two review executions.

---

#### `GET /api/v1/projects/{projectId}/reviews/{reviewId}/report`
- **Description**: Generates an aggregated analytics report for a review execution.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK`
  ```json
  {
    "reviewId": "987fcdeb-51a2-43d7-9012-3456789abcde",
    "projectId": "123e4567-e89b-12d3-a456-426614174000",
    "generatedAt": "2026-10-17T12:00:00Z",
    "status": "COMPLETED",
    "totalFindings": 3,
    "findingsBySeverity": {
      "WARNING": 2,
      "INFO": 1
    },
    "findingsByCategory": {
      "PERFORMANCE_AND_RELIABILITY": 2,
      "SCALABILITY": 1
    },
    "criticalFindings": [...],
    "keyRecommendations": [
      "Consider introducing read replicas, caching, or adopting the Database-per-Service pattern.",
      "Evaluate if a distributed cache (e.g., Redis) can reduce query latency."
    ]
  }
  ```

---

### 6. Finding Decisions & Recommendation Applier (`/api/v1/projects/{projectId}/findings`)

#### `PUT /api/v1/projects/{projectId}/findings/{findingId}/decision`
- **Description**: Records or updates an architectural decision on a finding (`ACCEPTED`, `DISMISSED`, `POSTPONED`).
- **Auth**: Authenticated & `Permission.REVIEW`
- **Request Body**:
  ```json
  {
    "decision": "ACCEPTED",
    "reason": "We will introduce Redis cache cluster in Sprint 4."
  }
  ```
- **Response**: `200 OK` (`DecisionResponse`)

---

#### `GET /api/v1/projects/{projectId}/findings/decisions`
- **Description**: Retrieves all saved decisions on findings for a project.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK` (`List<DecisionResponse>`)

---

#### `DELETE /api/v1/projects/{projectId}/findings/{findingId}/decision`
- **Description**: Removes an existing decision on a finding.
- **Auth**: Authenticated & `Permission.REVIEW`
- **Response**: `204 No Content`

---

#### `POST /api/v1/projects/{projectId}/findings/{findingId}/apply`
- **Description**: Generates an architectural recommendation preview patch for the finding.
- **Auth**: Authenticated & `Permission.REVIEW`
- **Response**: `200 OK`
  ```json
  {
    "findingId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "recommendation": "Evaluate if a distributed cache can reduce query latency.",
    "patchType": "ADD_CACHE_LAYER",
    "patchDescription": "Introduce a distributed caching layer (Redis) in front of datastores.",
    "previewPatch": {
      "action": "ADD_NODE",
      "proposedNodeType": "cache",
      "proposedLabel": "Redis Cache",
      "suggestedPosition": { "x": 250.0, "y": 100.0 },
      "targetAffectedNode": "node-2"
    }
  }
  ```
- **Implementation**:
  - `RecommendationApplier` generates structured proposed changes for client review, adhering to the rule that backend never mutates canvas architecture silently without approval.

---

#### `GET /api/v1/projects/{projectId}/findings/{findingId}/history`
- **Description**: Retrieves recorded decision history for a finding.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK` (`List<DecisionHistoryResponse>`)

---

### 7. Collaboration & Permissions (`/api/v1/projects/{projectId}/members` & `/api/v1/projects/{projectId}`)

#### `GET /api/v1/projects/{projectId}/members`
- **Description**: Lists all members and roles in a project.
- **Auth**: Authenticated & `Permission.READ`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "11111111-2222-3333-4444-555555555555",
      "projectId": "123e4567-e89b-12d3-a456-426614174000",
      "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "role": "OWNER",
      "createdAt": "2026-10-17T11:00:00Z"
    }
  ]
  ```

---

#### `PATCH /api/v1/projects/{projectId}/members/{userId}`
- **Description**: Changes a team member's role (`OWNER`, `ADMIN`, `EDITOR`, `REVIEWER`, `VIEWER`).
- **Auth**: Authenticated & `Permission.MANAGE_MEMBERS`
- **Request Body**:
  ```json
  {
    "role": "EDITOR"
  }
  ```
- **Response**: `200 OK` (`ProjectMemberResponse`)

---

#### `DELETE /api/v1/projects/{projectId}/members/{userId}`
- **Description**: Removes a member from the project.
- **Auth**: Authenticated & `Permission.MANAGE_MEMBERS`
- **Response**: `204 No Content`

---

#### `POST /api/v1/projects/{projectId}/leave`
- **Description**: Allows the current authenticated user to leave a shared project.
- **Auth**: Authenticated
- **Response**: `204 No Content`
- **Validation**: If user is the sole OWNER, throws `400 Bad Request` requiring ownership transfer or project deletion first.

---

#### `GET /api/v1/projects/{projectId}/permissions`
- **Description**: Returns the caller's role and effective permissions on the project.
- **Auth**: Authenticated
- **Response**: `200 OK`
  ```json
  {
    "projectId": "123e4567-e89b-12d3-a456-426614174000",
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "role": "OWNER",
    "permissions": ["READ", "EDIT", "REVIEW", "MANAGE_MEMBERS", "DELETE"]
  }
  ```

---

### 8. System & Observability (Spring Boot Actuator)

#### `GET /actuator/health`
- **Description**: Health check endpoint.
- **Auth**: Public
- **Response**: `200 OK`
  ```json
  {
    "status": "UP"
  }
  ```

#### `GET /actuator/info`
- **Description**: Application metadata and build details.
- **Auth**: Public
