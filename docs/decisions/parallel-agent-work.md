---
name: parallel-agent-work
date: 2026-09-26
---

Planned work is tracked as tbd beads; the roadmap is the four phase epics labelled
`roadmap`. When several agents work beads in parallel, they share the GitButler
workspace, each owns a disjoint set of files and commits to its own branch, and none
pushes or runs `tbd sync`. A branch that depends on another is stacked on it
(`but move <child> --above <parent>`).

**Why:** Disjoint files keep agents from breaking each other's builds; one reviewer
builds all branches together before syncing. Stacking was approved by the project owner
so dependent work does not need to be merged in a fixed order.

**How to apply:** Don't hand beads that touch Gradle build files to one agent while
others are building, and keep beads needing an owner decision (e.g. the license,
qk-ml8j) out of agent batches. Agents run on Sonnet, not Haiku: Haiku's qk-0vs7 fix
needed a rewrite although its report listed no problems. `/work-epic <epic-id>`
(`.claude/skills/work-epic/SKILL.md`) runs this whole process. In each brief:

- Assign every file an agent may need, including config files next to the one it fixes
  (e.g. `GeneratorConfig.kt` with `TraceGenerator.kt`), and keep shared public APIs
  source-compatible so `example/` still compiles.
- Commit with explicit IDs (`but commit -b <branch> <ids>`); without IDs the commit takes
  the other agents' uncommitted changes.
- Run only your own tests, scoped to the module (`:core:test --tests Foo`), while others
  edit.
- Beads that change the same file go to one agent, and it edits in stages so each bead
  gets its own commit; hunks from one combined edit can't be split by bead afterwards.
