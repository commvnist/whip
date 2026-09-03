# VERA-Codex orchestration policy

Apply these rules to all development work in every workspace governed by this file.

## Default activation and standing authorization

VERA-Codex is the default development process. Automatically classify and route every request that creates, changes, repairs, refactors, tests, builds, packages, migrates, or deploys software. The user does not need to name VERA-Codex or request agents again.

The user grants standing authorization to spawn the VERA roles required by this policy for development work. Begin VERA routing before substantive investigation or editing. Do not bypass a required route merely because delegation was not repeated in the current prompt.

This standing authorization does not authorize destructive, irreversible, credential, production, billing, publication, or external side effects beyond the task's scope. Those approval boundaries remain in force.

Trivial work may remain in the parent when the explicit `direct_parent` rule applies. That is a VERA-routed decision, not an exemption from VERA. Status, explanation, and other read-only conversational requests are not development work unless they also request a software change.

## Objective

Minimize total tokens, cost, and wall-clock time subject to the task's quality and safety requirements. Use the least expensive eligible model, verify its work, and escalate only on explicit risk or failed evidence.

## Task contract

Before delegating, record the intended behavior, non-goals, affected boundary, risk class, acceptance checks, and approval constraints. Give subagents only the contract and relevant context.

## Risk gates

Classify as high or critical when work touches security boundaries, secrets, authorization, cryptography, schema/data migration, destructive operations, deployment, billing, public compatibility, distributed state, concurrency, or three or more tightly coupled subsystems. Also classify as high when requirements conflict, reproduction is unavailable, verification is weak, or two lower-tier attempts fail materially.

For high/critical work:

- Ask `sol_architect` for a read-only plan before implementation.
- Use `sol_critical_builder` for the implementation when the change itself requires premium reasoning.
- Ask `sol_architect` for a fresh, independent final review after deterministic checks pass.
- Require user approval for irreversible, production, credential, or external side effects.

## Default routing

- Use `luna_scout` for bounded read-only file discovery, symbol maps, documentation extraction, log reduction, and independent test-gap searches.
- Use `terra_builder` for localized bug fixes, tests, routine refactors, and ordinary multi-file changes.
- Use `terra_reviewer` for independent review of medium-risk changes.
- Keep trivial work in the parent when delegation overhead would exceed the work.
- Do not use a premium agent for summarization, formatting, status, or raw search.

## Parallelism

- Spawn at most three independent read-only scouts at once.
- Give each scout a non-overlapping question and require concise path/symbol evidence.
- Parallelize independent checks when safe.
- Serialize edits unless ownership is file-disjoint and interfaces are stable.
- Never let two agents edit the same file. Each writer must know that other agents may be working and must preserve their changes.
- Wait for every relevant delegated result before deciding.

## Verification and escalation

After a change, run the narrowest relevant reproduction, targeted tests, type/lint/build checks, regression checks, and diff-scope review.

On the first material failure, give the same agent one repair attempt using only a compact failure capsule: task contract, changed diff, failing command, causal error, and relevant file paths. If it fails again, or evidence conflicts, escalate one tier with fresh context. Do not run unbounded self-reflection or retry loops.

Low-risk work may finish after objective checks pass. Medium-risk work also requires `terra_reviewer`. High/critical work requires a fresh `sol_architect` review. Stop once all required checks and review gates pass.

## Evidence capsule

Every subagent returns no more than:

1. Conclusion.
2. Evidence with exact paths and symbols.
3. Commands/checks run and their result.
4. Remaining uncertainty.
5. Recommended next action.

Do not return full logs, restate the task, or include generic advice.

## Evaluation

Record per task: route, model, effort, input/cached/output/reasoning tokens, latency, retries, checks, review outcome, final acceptance, and escaped defect severity. Treat routing thresholds as provisional until validated against a frozen representative task set.
