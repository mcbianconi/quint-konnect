---
type: is
id: is-01m3e5ad9x9mj8v135bn9vypym
title: Gradle plugin downloads quint
kind: feature
status: closed
priority: 2
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T06:09:26.845Z
updated_at: 2026-09-26T19:59:42.732Z
closed_at: 2026-09-26T19:59:42.732Z
close_reason: "Added gradle-plugin/downloadQuint (opt-in, default false): downloads quint's standalone per-OS/arch GitHub release binary (macOS/Linux) into the Gradle user home cache, verifies sha256 for the pinned 0.32.0 default, wires Test tasks and checkQuint to use it via core's quintkonnect.quintExecutable system property. Commit mzo on branch plugin-quint-runtime."
resolution: null
duplicate_of: null
---
Plugin downloads the pinned quint into the Gradle user home (like node-gradle-plugin) so a fresh clone needs no Node/quint install.
