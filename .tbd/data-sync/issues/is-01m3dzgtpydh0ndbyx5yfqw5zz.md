---
type: is
id: is-01m3dzgtpydh0ndbyx5yfqw5zz
title: Fix README drift and fill AGENTS.md
kind: chore
status: closed
priority: 2
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T04:28:05.725Z
updated_at: 2026-09-26T06:20:52.078Z
closed_at: 2026-09-26T06:20:52.077Z
close_reason: Fixed maxSamples drift in README, split QuintRun/QuintTest param tables, noted @QuintTest broken (qk-cxf6), filled AGENTS.md Build & Test / Architecture Overview / Conventions. Committed as kyl on docs-license-ci.
resolution: null
duplicate_of: null
---
README says maxSamples default is 5; code default is 100 (DEFAULT_TRACES, annotation passes -1). AGENTS.md build/architecture sections are placeholders.
