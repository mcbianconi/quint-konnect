---
type: is
id: is-01m3e7dfmwx3g40p4shw07ke7b
title: Extract ITF into an :itf module (io.github.mcbianconi.itf)
kind: task
status: closed
priority: 1
version: 7
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e7dfwyvcdbqdj15rrb4ta2
  - type: blocks
    target: is-01m3dzh3h36exkjke6fkw32cph
  - type: blocks
    target: is-01m3e5aepym9ckf6pk2jkpfty9
  - type: blocks
    target: is-01m3e5ae0ashs5x7r8bs4nk1c1
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T06:46:04.694Z
updated_at: 2026-09-26T06:57:34.651Z
closed_at: 2026-09-26T06:57:34.647Z
close_reason: 'Extracted ITF (ItfValue, ItfTrace, ItfValueSerializer, ItfValueDisplay, ItfValueNormalizer, BigIntegerSerializer) into new :itf module, package io.github.mcbianconi.itf. core depends via api(project(":itf")). Committed as itf-module (kzq). Build green: 84 passed/2 skipped (itf:32, core:49, example:5).'
resolution: null
duplicate_of: null
---
Move core/.../quintkonnect/itf/*.kt (ItfValue, ItfTrace/parseTrace, ItfValueSerializer, ItfValueDisplay, ItfValueNormalizer, BigIntegerSerializer) into a new Gradle module :itf, package io.github.mcbianconi.itf, artifact io.github.mcbianconi:itf-kotlin (same repo, same version and release as quint-konnect). Move ItfValueNormalizerTest and ItfValueSerializerTest to itf/src/test. Use the build-logic convention plugin (qk-29tt) plus kotlinx-serialization. core depends on it with api(project(":itf")) because ItfValue is in core's API (State.check, NondetPicks.get, Step.state). Update imports in core, example, KSP-generated code (StepMethodGenerator) and README/CLAUDE.md snippets (BigIntegerSerializer import). Pure move and rename, no behavior change; :itf:test, :core:test, :example:test green. Add docs/decisions/itf-module.md (why a separate module: mirrors upstream quint-connect depending on the itf-rs crate, reusable by other JVM ITF producers such as Apalache; package/artifact names; same-repo/same-version rule) plus its index line.
