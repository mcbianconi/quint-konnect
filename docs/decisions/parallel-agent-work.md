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
qk-ml8j) out of agent batches.
