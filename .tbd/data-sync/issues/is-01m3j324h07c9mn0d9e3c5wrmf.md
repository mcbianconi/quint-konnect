---
type: is
id: is-01m3j324h07c9mn0d9e3c5wrmf
title: Spec types file lists only the first driver as originating file
kind: bug
status: closed
priority: 3
version: 3
delegate: claude-code@macmurillo.local
labels:
  - roadmap
dependencies: []
parent_id: is-01m3e5aa1sevc19rem5gdd0hye
hold: null
hold_until: null
created_at: 2026-09-27T18:46:56.287Z
updated_at: 2026-09-27T19:38:58.726Z
started_at: 2026-09-27T19:36:12.484Z
closed_at: 2026-09-27T19:38:58.724Z
close_reason: Spec types are written once per round with every sharing driver as an originating file; verified against KSP's DirtinessPropagator and covered by a recording-CodeGenerator test.
resolution: null
duplicate_of: null
---
SpecTypesGenerator writes one <Module>Spec.kt per (package, module) with aggregating=false and only the first driver's containing file as origin. With two drivers sharing a spec in one package, KSP incremental processing may drop or fail to regenerate the file when only the second driver changes or the first is deleted. Not reproduced; example has no such pair. Check KSP's incremental behaviour and add every sharing driver as an originating file (or aggregate).
