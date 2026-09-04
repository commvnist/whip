---
name: vera-codex
description: Route and verify repository software implementation, testing, non-trivial diagnosis, and code review; not general status or explanation requests.
---

# VERA-Codex

Keep the central parent responsible for scope, routing, integration, and acceptance. Use subagents only for explicitly bounded work. Respect the user's scope and constraints, applicable higher-priority instructions, and every other triggered skill.

## Establish the task contract

Before substantive investigation or editing:

1. Record the intended behavior and explicit non-goals.
2. Record the affected files, interfaces, and ownership boundary.
3. Classify risk as trivial, low, medium, high, or critical.
4. Name executable acceptance checks or observable success criteria.
5. Record approval constraints for irreversible, production, credential, billing, publication, deployment, or external effects.
6. Snapshot pre-existing changes with a targeted status and diff. Preserve them throughout the task.

Do not expand scope merely to make routing easier. Treat absent evidence as uncertainty, not permission.

## Apply hard gates first

Classify work as high or critical before considering a cheaper route when it touches any of these conditions:

- Security boundaries, authentication, authorization, secrets, cryptography, or sandbox controls.
- Schema or data migration, destructive state, deployment controls, billing, or rollback-sensitive state.
- Public compatibility, shared protocols, concurrency, or distributed state.
- Three or more tightly coupled subsystems.
- Conflicting requirements, unavailable reproduction, or weak verification.
- Two material lower-tier failures.

Classify irreversible, production, credential, billing, publication, deployment, or other external actions as critical. Perform safe planning, local implementation, and reversible validation without pausing; request explicit user approval only when the final actionable boundary would create the protected effect.

If classification remains unclear, assign one bounded question to `terra_scout`. If uncertainty remains after that discovery, classify the task as high. Do not repeatedly scout the same uncertainty.

## Route the work

- Keep genuinely trivial work in the parent when delegation overhead exceeds the work.
- Assign low-risk implementation to one `terra_builder`.
- Assign medium-risk implementation to one `terra_builder`, then require a fresh `terra_reviewer` after checks pass.
- For high- or critical-risk work, require a fresh `sol_architect` plan, assign the approved plan to one `sol_critical_builder`, run deterministic checks, then require a new `sol_architect` instance for final review.

Use only runtime-confirmed `gpt-5.6-terra` and `gpt-5.6-sol` roles from this repository. Keep exactly one writer active at a time. Serialize dependent work and integration. Run at most three complementary read-only agents concurrently, and only when their questions are independent. Do not use homogeneous voting, debate, or duplicate searches by default.

### Spawn roles with explicit runtime intent

Every new role spawn must explicitly pass `model`, `reasoning_effort`, and `fork_turns` using this matrix:

| Role | Model | Reasoning effort |
|---|---|---|
| `terra_scout` | `gpt-5.6-terra` | `low` |
| `terra_builder` | `gpt-5.6-terra` | `medium` |
| `terra_reviewer` | `gpt-5.6-terra` | `high` |
| `sol_architect` | `gpt-5.6-sol` | `xhigh` |
| `sol_critical_builder` | `gpt-5.6-sol` | `xhigh` |

Set `fork_turns="none"` by default. Use only a small positive integer when task-local conversation context is essential; never use `fork_turns="all"` and never omit `fork_turns`. The parent must include the task contract plus only relevant evidence in the spawn message.

Explicit spawn values make routing intent auditable and establish the pre-role/default selection; they do not override a matching custom role TOML. Codex first resolves each setting from the explicit spawn value, then the corresponding `[agents]` default, then the parent's value. It applies the custom role TOML as the final configuration layer, so `model` and `model_reasoning_effort` in that file take precedence. The bundle validator keeps the explicit matrix above and the role TOMLs identical; that enforced identity protects final model and effort selection.

## Verify and accept

Run the narrowest relevant checks in this order:

1. Reproduce the failure or define the observable behavior.
2. Run targeted tests for the changed behavior.
3. Run affected type, lint, build, or static checks.
4. Run relevant regression or contract checks.
5. Inspect diff scope and generated artifacts.
6. Pass the fresh review gate required by the route.

Never substitute model confidence for executable evidence. Accept only after required checks pass, the fresh reviewer reports no blocking finding, the diff remains in scope, and any protected final action has explicit approval.

## Repair and escalate

On the first material check or review failure, return the work to the same writer once. Give that writer only the task contract, changed diff, failing command, causal error, and relevant paths. Rerun deterministic checks and use a fresh risk-appropriate reviewer.

On a second material lower-tier failure, escalate to the Sol route with a compact failure capsule and fresh context. On a second material Sol failure, stop retries and report the blocker, evidence, and approval or input needed.

An unresolved material failure takes precedence over a protected-action approval boundary: complete the required repair or escalation, or report the blocker, while approval remains deferred. Use `await_user_approval` only when no unresolved material failure exists.

Stop when the acceptance gates pass. Do not add speculative reviewers or retries without a named failed criterion.

## Return evidence capsules

Require every subagent response to contain only:

1. Conclusion.
2. Evidence with exact paths and symbols.
3. Commands/checks run and their result.
4. Remaining uncertainty.
5. Recommended next action.

Record route, model, effort, latency, token usage, retries, checks, review outcome, acceptance, and escaped-defect severity only when the runtime actually exposes those values. Never estimate or invent metrics.
