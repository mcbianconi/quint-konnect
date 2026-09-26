---
type: is
id: is-01m3e5adhcxqj4qjc9x4emcrmm
title: Runtime overrides for annotation parameters
kind: feature
status: closed
priority: 2
version: 4
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T06:09:27.083Z
updated_at: 2026-09-26T20:06:22.654Z
closed_at: 2026-09-26T20:06:22.653Z
close_reason: Added -Pquint.maxSamples/-Pquint.maxSteps/-Pquint.seed/-Pquint.verbose Gradle properties mapped to quintkonnect.* system properties via a CommandLineArgumentProvider (Test-task input, validated at configuration time). core's RunConfig/TestConfig.nTraces and RunConfig's --max-steps, plus ConsoleReplayListener's no-arg constructor, read the override ahead of the annotation/env-var value. seed override only applies when the annotation leaves seed blank (KSP bakes an explicit literal in directly; documented in Seed.kt as a KSP/ABI-constrained deviation). Commit kxy on branch plugin-quint-runtime.
resolution: null
duplicate_of: null
---
Annotations are @Retention(SOURCE); changing maxSamples/maxSteps needs a recompile and only seed/verbosity are runtime env vars. Support -Pquint.maxSamples etc. and a Gradle property for verbosity, for PR vs nightly CI profiles. Once qk-adm2 lands these are inputs of generateQuintTraces.
