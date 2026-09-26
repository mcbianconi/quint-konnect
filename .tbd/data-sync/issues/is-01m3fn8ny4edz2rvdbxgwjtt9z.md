---
type: is
id: is-01m3fn8ny4edz2rvdbxgwjtt9z
title: Let -Pquint.seed override a seed fixed in @QuintRun/@QuintTest
kind: task
status: closed
priority: 3
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T20:07:21.795Z
updated_at: 2026-09-26T20:22:31.577Z
closed_at: 2026-09-26T20:22:31.577Z
close_reason: override > non-blank annotation seed > QUINT_SEED > random via new genSeed(annotationSeed) overload; generators always call genSeed(...). Committed as rmt on branch seed-override.
resolution: null
duplicate_of: null
---
qk-eigq made -Pquint.seed (system property quintkonnect.seed) win over QUINT_SEED and the random default, but a non-blank annotation seed is baked by the KSP generators into RunConfig(seed = "...") and never reaches genSeed(). Pass the annotation seed through a public helper (e.g. genSeed(fallback)) or have the generator emit the override lookup, keeping RunConfig/TestConfig binary compatible.
