---
type: is
id: is-01m3e5adhcxqj4qjc9x4emcrmm
title: Runtime overrides for annotation parameters
kind: feature
status: open
priority: 2
version: 2
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T06:09:27.083Z
updated_at: 2026-09-26T06:49:38.597Z
---
Annotations are @Retention(SOURCE); changing maxSamples/maxSteps needs a recompile and only seed/verbosity are runtime env vars. Support -Pquint.maxSamples etc. and a Gradle property for verbosity, for PR vs nightly CI profiles. Once qk-adm2 lands these are inputs of generateQuintTraces.
