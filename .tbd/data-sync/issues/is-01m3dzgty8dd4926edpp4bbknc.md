---
type: is
id: is-01m3dzgty8dd4926edpp4bbknc
title: KSP compile-time validation of drivers
kind: feature
status: closed
priority: 2
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T04:28:05.960Z
updated_at: 2026-09-26T14:43:35.590Z
closed_at: 2026-09-26T14:43:35.590Z
close_reason: "Implemented: non-public @QuintAction/no-arg-constructor validation, getAllFunctions()+findOverridee() for inherited actions, tests added"
resolution: null
duplicate_of: null
---
Report KSP errors for non-public @QuintAction functions, duplicate action names, missing no-arg constructor. Include inherited actions (only getDeclaredFunctions() is scanned today).
