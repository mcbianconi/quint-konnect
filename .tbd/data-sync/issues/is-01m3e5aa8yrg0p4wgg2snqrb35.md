---
type: is
id: is-01m3e5aa8yrg0p4wgg2snqrb35
title: Read quint typed IR at build time
kind: task
status: open
priority: 3
version: 3
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e5aag6b4dd6gnkz2jn871x
  - type: blocks
    target: is-01m3e5aaqvhketzpn1h4662zt3
parent_id: is-01m3e5aa1sevc19rem5gdd0hye
created_at: 2026-09-26T06:09:23.741Z
updated_at: 2026-09-26T06:09:24.218Z
---
Run quint compile --target json (or typecheck --out) at build time and expose action names, nondet names and types to KSP. Available in quint 0.32.0.
