# Connecting AI agents (GPT, Claude, Copilot, Kimi, local models)

1. A project owner/admin opens the editor → **Agents** → picks the model, a name and a permission
   (EDITOR / REVIEWER / VIEWER) → **Connect**. The token (`amk_...`) is shown once.
   API: `POST /api/v1/projects/{projectId}/agents {"name","provider","role"}`.
2. Give the model the token and the instructions shown in the dialog.
3. The agent edits like a person; every change appears live for everyone in the project.

Agent tokens only work for their own project. They can never manage members, agents or invitations,
archive or delete the project. Revoking (`DELETE /api/v1/projects/{projectId}/agents/{agentId}`) cuts access immediately.

## Endpoints an agent uses

| Call | Purpose |
|---|---|
| `GET /api/v1/agent/me` | project id, role, endpoint list and operation examples |
| `GET /api/v1/projects/{id}/architecture` | current diagram (`nodes`, `edges`) |
| `POST /api/v1/projects/{id}/architecture/operations` | conflict-free edits (below) |
| `GET /api/v1/architecture/component-types` | allowed component types |
| `WS /ws/projects/{id}?token=amk_...` | optional live channel (presence, cursors, other people's edits) |

## Operations

```json
{"operations": [
  {"op":"node.add","node":{"id":"orders","type":"service","label":"Order Service","x":300,"y":120}},
  {"op":"node.update","id":"orders","patch":{"label":"Orders","x":320}},
  {"op":"edge.add","edge":{"id":"e1","sourceId":"gateway","targetId":"orders","type":"sync","label":"REST"}},
  {"op":"edge.update","id":"e1","patch":{"label":"gRPC"}},
  {"op":"edge.remove","id":"e1"},
  {"op":"node.remove","id":"orders"}
]}
```

Operations are applied to the latest document (no revision needed), so many people and agents can edit at once.
Every accepted batch is broadcast to live clients as `operations.applied`.

## Live socket events

Server → client: `presence`, `user.joined`, `user.left`, `cursor.moved`, `selection.changed`,
`operations.applied`, `architecture.updated`. Client → server: `cursor.moved {x,y}`, `selection.changed {id}`.

## Code generation

`GET /api/v1/projects/{id}/codegen?language=java&layout=multi-module|per-service&groupId=com.example` → zip.
`GET .../codegen/preview` → the same files as JSON. Generators implement `codegen.CodeGenerator`;
add a new implementation (for example an LLM-backed one) to support other languages.
