---
type: is
id: is-01m3e5abnwyqq8tajfbmjbjp6g
title: Trace generation as a cacheable Gradle task
kind: feature
status: open
priority: 3
version: 3
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e5ackze94enpnzzrwnntm2
parent_id: is-01m3e5a9sj92e7ptv02j419tm8
created_at: 2026-09-26T06:09:25.180Z
updated_at: 2026-09-26T06:49:37.087Z
---
Plugin registers generateQuintTraces with inputs spec, params, seed, quint version and output build/quint-konnect/traces/ (failures go to build/quint-konnect/failures/, qk-ci0y). Tests only replay files: up-to-date checks, build cache, replay becomes the single path, no full List<ItfTrace> in memory. Runtime overrides (qk-eigq) are task inputs; shrinking (qk-a8ay) is a separate task.
