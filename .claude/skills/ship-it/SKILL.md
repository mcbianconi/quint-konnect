---
name: ship-it
description: Land all finished GitButler branches onto origin/main and push, then watch CI and sync tbd. Use when the user says "ship it", "land everything", or "land and push".
---

# Ship it

Saying "ship it" authorizes landing and pushing the applied branches of this session. It does not
authorize landing another agent's in-progress branch; ask about those.

## 1. Collect the work

1. `but status -fv` — list applied stacks, commits and uncommitted changes.
2. Commit uncommitted session work onto the branch it belongs to (`but amend -t` or
   `but commit -b`). Docs-only or config-only changes with no matching branch go on their own
   branch. Ask if the placement is ambiguous; never land half-committed work.
3. Check every commit has the `Co-Authored-By` trailer when an agent wrote it
   (`git log -1 --format=%B <sha>`); fix with `but reword <id> -m "..."`.
4. Branches that depend on each other must be stacked (`but move <child> --above <parent>`),
   see `docs/decisions/parallel-agent-work.md`.

## 2. Verify before landing

1. Run the full build through the Gradle MCP tool with every branch applied:
   `gradle build --rerun-tasks` (envSource SHELL). Stop and report on any failure.
2. If other agents' branches are applied, run `but pull --check` and ask before continuing when
   it reports conflicts.

## 3. Land

Land one stack at a time, bottom-up dependencies first:

- single branch: `but land <branch> --yes`
- stack: `but land <top-branch> --whole-stack --yes`

`but land` fast-forwards or merges onto the target, pushes it, reconciles the remaining branches
and deletes landed remote branches. If a later land reports conflicted commits, stop and ask.

## 4. After landing

1. `gh run list --branch main --limit 3`, then `gh run watch <run-id> --exit-status` for the
   run of the landed commit. Report failures with the log excerpt (`gh run view <id> --log-failed`)
   and do not claim success until it passes.
2. Close finished beads in one call (`tbd close <ids...> --reason "..."`), then `tbd sync`.
3. Summarize: what landed (commit subjects), the CI result, and any beads left open.
