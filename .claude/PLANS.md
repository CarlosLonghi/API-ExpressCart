# ExecPlans: Specification for Complex Work

An ExecPlan is a self-contained working document for a feature or refactor big enough to benefit from writing the plan down: it captures the goal, the plan, the decisions made along the way and what shipped. Store one per non-trivial piece of work in `.claude/execplans/YYYY-MM-DD-name.md`, copied from `.claude/execplans/TEMPLATE.md`.

## When to write one

- **Each refactor phase** from `docs/REFACTORING.md` §6
- New endpoints or entities
- Changes crossing 3+ modules (controller + service + repository, etc.)
- Changes to the HTTP contract (status codes, payloads, routes)
- Infra changes (cache, Mongo indexes, Feign config, Docker)

Skip it for trivial bug fixes, doc-only changes or single-file tweaks.

## Sections

1. **Goal**: 1-2 sentences: what is true after this ships?
2. **Big picture**: current state, desired state, why now, affected modules, `docs/REFACTORING.md` item ids covered.
3. **Progress checklist**: track work items as you go.
4. **Surprises & discoveries**: timestamped notes on anything unexpected.
5. **Decision log**: `date | decision | rationale`. This is the part future agents actually reread, so write it like a code-review comment, not a form.
6. **Context & orientation**: assumptions, key files, related docs.
7. **Plan of work**: concrete file-by-file steps.
8. **Concrete steps**: copy-paste-ready commands with expected output.
9. **Validation & acceptance**: criteria and test scenarios.
10. **Outcomes & retrospective**: what shipped, what was learned, what's left.

## Working with an ExecPlan

- Update it live (Progress/Discoveries/Decisions as you go, not just at the end).
- Reference exact files and methods, not vague descriptions.
- Cross-link `.claude/GLOSSARY.md`, `.claude/ARCHITECTURE.md` and `docs/REFACTORING.md` instead of restating them.
- If you find something that changes the shared docs (a new invariant, a fixed bug, a corrected assumption), update `.claude/ARCHITECTURE.md`, `docs/ARCHITECTURE.md` or `docs/REFACTORING.md` in the same change, so it doesn't stay buried only in the ExecPlan.
