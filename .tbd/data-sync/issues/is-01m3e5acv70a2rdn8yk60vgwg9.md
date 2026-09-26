---
type: is
id: is-01m3e5acv70a2rdn8yk60vgwg9
title: Support suspend @QuintAction functions
kind: feature
status: closed
priority: 2
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T06:09:26.374Z
updated_at: 2026-09-26T15:22:50.802Z
closed_at: 2026-09-26T15:22:50.801Z
close_reason: "Committed lws on typed-driver: generatedStep wraps suspend @QuintAction calls in kotlinx.coroutines.runBlocking; processor errors clearly if runBlocking isn't resolvable; added suspending example driver."
resolution: null
duplicate_of: null
---
generatedStep is not suspend and emits this.fn(args), so suspend @QuintAction functions fail to compile. Generate a suspend dispatcher and run it with runTest.
