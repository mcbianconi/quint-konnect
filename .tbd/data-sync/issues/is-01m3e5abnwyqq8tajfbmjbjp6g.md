---
type: is
id: is-01m3e5abnwyqq8tajfbmjbjp6g
title: Trace generation as a cacheable Gradle task
kind: feature
status: open
priority: 3
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3e5a9sj92e7ptv02j419tm8
created_at: 2026-09-26T06:09:25.180Z
updated_at: 2026-09-26T06:09:25.180Z
---
Plugin registers generateQuintTraces with inputs spec, params, seed, quint version and output build/quint-traces/. Tests only replay files: up-to-date checks, build cache, replay becomes the single path, no full List<ItfTrace> in memory.
