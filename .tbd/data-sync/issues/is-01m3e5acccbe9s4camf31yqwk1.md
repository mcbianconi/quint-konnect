---
type: is
id: is-01m3e5acccbe9s4camf31yqwk1
title: Save failing traces and print a replay command
kind: feature
status: closed
priority: 2
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T06:09:25.900Z
updated_at: 2026-09-27T12:56:09.447Z
closed_at: 2026-09-27T12:56:09.442Z
close_reason: null
resolution: null
duplicate_of: null
---
A seed only reproduces with the same spec and quint version, and traces are deleted with the temp dir. Write the failing trace to build/quint-konnect/failures/<test>-<trace>.itf.json and print the replay command.
