---
type: is
id: is-01m3e5ac50k1ks6sbctrx4q56t
title: Enable explicit API mode and ABI validation
kind: chore
status: closed
priority: 1
version: 6
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3dzh3h36exkjke6fkw32cph
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T06:09:25.663Z
updated_at: 2026-09-26T12:42:54.643Z
closed_at: 2026-09-26T12:42:54.643Z
close_reason: "Enabled explicit API mode + Kotlin Gradle plugin abiValidation() for annotations/itf/core/ksp via a quintkonnect.library convention plugin; visibility set per the decided surface; ABI dumps committed at <module>/api/<module>.api; checkKotlinAbi verified failing on a signature change and passing after revert; full build --rerun-tasks: 93 tests passed, 0 skipped."
resolution: null
duplicate_of: null
---
No explicitApi() or ABI validation in any build script. NondetPicks.get returns ItfValue, leaking the ITF model. Decide the public surface and make the rest internal before publishing.

## Notes

Decided public surface (derived from KSP-generated code and example usage; generated code runs in the user's module so everything it calls must be public):
- :annotations: all public.
- :itf: as set in qk-pzl0 (parseTrace, ItfTrace, ItfState, ItfValue + variants, intoOption, display, ItfValueSerializer, BigIntegerSerializer, ItfValue.decode x2).
- :core public: Driver, DriverConfig, State, TypedState, Step (read-only properties), NondetPicks + get/isEmpty + decode/decodeOrNull, Runner.runTest(driverFactory, generatorConfig, testName), GeneratorConfig, DEFAULT_TRACES, RunConfig, TestConfig, genSeed.
- :core internal: Logger, TraceGenerator, Step.fromState, NondetPicks.fromRecord, Runner.runTest(..., traces).
- :ksp: QuintKonnectProcessorProvider public (ServiceLoader); processor and generators internal.
ABI validation: prefer the Kotlin Gradle plugin's built-in abiValidation (Kotlin 2.4) over kotlinx binary-compatibility-validator if it works; commit the dump files. Previously: after qk-km9v/qk-pzl0, NondetPicks.get returning ItfValue is fine: ItfValue is the published :itf API.
