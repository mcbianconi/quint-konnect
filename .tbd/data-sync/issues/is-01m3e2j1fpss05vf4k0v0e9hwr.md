---
type: is
id: is-01m3e2j1fpss05vf4k0v0e9hwr
title: Make skill type table canonical and add sync rule
kind: chore
status: open
priority: 2
version: 4
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e2j2kdtvex13knhqjv1703
  - type: blocks
    target: is-01m3e5ae7vcqt89dehgb65ep30
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T05:21:11.158Z
updated_at: 2026-09-26T06:49:37.623Z
---
README.md and CLAUDE.md link to skills/quint-konnect/references/types.md instead of carrying copies. Add rule to AGENTS.md/CLAUDE.md: changes to annotations, Driver, TypedState or env vars must update skills/quint-konnect/.
