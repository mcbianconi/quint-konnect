---
type: is
id: is-01m3j324s6q0a1nmpeaj7awc6z
title: Generate monomorphized classes for generic typedefs
kind: feature
status: open
priority: 4
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3e5aa1sevc19rem5gdd0hye
created_at: 2026-09-27T18:46:56.550Z
updated_at: 2026-09-27T18:46:56.550Z
---
SpecTypesGenerator skips generic typedefs other than Option (e.g. type Opt[a] = Present(a) | Absent applied as Opt[Coin]) and everything containing them, with a warning. Substituting the type params per application would cover them.
