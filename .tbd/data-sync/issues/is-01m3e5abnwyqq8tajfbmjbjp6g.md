---
type: is
id: is-01m3e5abnwyqq8tajfbmjbjp6g
title: Trace generation as a cacheable Gradle task
kind: feature
status: closed
priority: 3
version: 4
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e5ackze94enpnzzrwnntm2
parent_id: is-01m3e5a9sj92e7ptv02j419tm8
created_at: 2026-09-26T06:09:25.180Z
updated_at: 2026-09-27T18:13:53.819Z
closed_at: 2026-09-27T18:13:53.818Z
close_reason: "Added generateQuintTraces (gradle-plugin, GenerateQuintTracesTask): reads per-driver JSON manifests KSP writes (ksp/.../generators/DriverManifestWriter.kt), runs quint once per driver into build/quint-konnect/traces/<Driver>/, cacheable when every driver's seed is pinned. Test tasks depend on it (unless -Pquint.replay) and get quintkonnect.tracesDir; core's ReplayRunner/TracesDirTraceSource replays that dir when present, falling back to invoking quint otherwise (example unaffected). Design in docs/decisions/generate-quint-traces-task.md. Branch trace-task, commits xzn/pyz/tsl."
resolution: null
duplicate_of: null
---
Plugin registers generateQuintTraces with inputs spec, params, seed, quint version and output build/quint-konnect/traces/ (failures go to build/quint-konnect/failures/, qk-ci0y). Tests only replay files: up-to-date checks, build cache, replay becomes the single path, no full List<ItfTrace> in memory. Runtime overrides (qk-eigq) are task inputs; shrinking (qk-a8ay) is a separate task.
