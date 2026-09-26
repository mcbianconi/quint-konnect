---
name: work-epic
description: Finish the open beads of a tbd epic with parallel Sonnet subagents, one GitButler branch each, then review, build, close the epic and record lessons. Use when the user runs /work-epic <epic-id>.
argument-hint: <epic-id>
disable-model-invocation: true
---

# Work an epic

Epic: `$ARGUMENTS`. Agents run on Sonnet (`model: sonnet`), never Haiku — a Haiku agent
once reported a clean fix (qk-0vs7) that needed a rewrite.

## 1. Plan (no edits yet)

1. `tbd show $ARGUMENTS`, then `tbd list --parent $ARGUMENTS --all`; read each open child with
   `tbd show <id>`. Check `but status` is clean or holds only known branches.
2. Read the files each bead names and find which files it must change, including neighbours
   (config interfaces, tests, README/CLAUDE.md type tables, `docs/decisions/`).
3. Group beads so that no two agents share a file. Beads touching the same file go to one
   agent, done in a fixed order. Leave out beads that need an owner decision or touch Gradle
   build files while others build; list them for the user.
4. For each agent, decide how its fix is tested with no new dependencies (e.g. inject a
   command through an interface, add a fixture under a new `example/` package).
5. Show the grouping as a table (agent, beads, owned files, branch, test plan) and get the
   user's approval before spawning.

## 2. Spawn

One `general-purpose` agent per group, `model: sonnet`, all in one message, run in the
background. Each brief contains:

- the bead IDs and order, the bead text, and the design hints from step 1;
- read `CLAUDE.md`, `AGENTS.md`, `docs/decisions/*`; mark each bead `in_progress`;
- the owned files; everything else is off-limits, including `Runner.kt`, existing
  `example/` files and Gradle build files (stop and report if a dependency is needed);
- keep public call sites source-compatible (overloads or default parameters);
- tests through the Gradle MCP, scoped to the module and own classes
  (`:core:test --tests Foo`); if a file it doesn't own fails to compile, wait and retry;
- load the `gitbutler` skill; commit with explicit IDs only,
  `but commit -b <branch> -m "type(scope): summary" <ids>`, one commit per bead (edit in
  stages when beads share a file), with the Co-Authored-By trailer; never commit without
  IDs, never push, never `tbd sync`, never touch other agents' changes;
- if its work builds on an unlanded branch (e.g. a new module needs a convention plugin
  from another branch), create its branch stacked before the first commit:
  `but branch new <branch> --above <dependency-branch>` (a plain `but commit -b` is
  refused when the changes touch files committed on that branch);
- after each commit, check `but status` shows no leftover changes to files it committed,
  and compare them with the commit (`git show <sha>:<path>`);
- close each bead after its commit (`tbd close <id> --reason "..."`); add a
  `docs/decisions/` entry only for a standing decision that isn't already visible in
  code, tests, or other docs — otherwise put it there instead (see
  `docs/decisions/README.md`);
- a final report: beads closed, commits, test results, design summary, and a **Problems**
  section (tool failures, doc gaps, GitButler/tbd/Gradle MCP quirks, conflicts, guessed
  decisions).

## 3. Review

As each agent finishes:

1. Read its whole diff (`git show <sha>`), not only the report. Fix problems yourself and
   `but amend -t <commit> <ids>` into the agent's commit.
2. When all are done, run `build` through the Gradle MCP with every branch applied. Stop and
   report on failures.
3. Confirm all children are closed, then `tbd close $ARGUMENTS --reason "..."`.
4. Out-of-scope bugs agents found become new beads under the right phase epic.

## 4. Record lessons

Turn the Problems sections and review findings into memory files (tool quirks, agent
behaviour) and `docs/decisions/` entries (standing process rules). Commit repo doc changes on
their own branch with explicit IDs.

## 5. Report

A table of branches with beads and results, the build result, new beads, and where each
lesson was recorded. Don't push; the user runs `/ship-it` to land.
