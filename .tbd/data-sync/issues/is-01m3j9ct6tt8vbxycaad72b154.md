---
type: is
id: is-01m3j9ct6tt8vbxycaad72b154
title: Replaying with -Pquint.replay breaks test compilation when readSpecIr is true
kind: bug
status: closed
priority: 2
version: 2
labels: []
dependencies: []
created_at: 2026-09-27T20:37:37.626Z
updated_at: 2026-09-27T23:36:35.039Z
closed_at: 2026-09-27T23:36:35.033Z
close_reason: "Fixed in mcbianconi/quint-konnect#10 (merged): wireQuintIr now always wires quintIr into KSP when readSpecIr is on, regardless of -Pquint.replay."
resolution: null
duplicate_of: null
---
Found while making example/ apply the plugin (qk-hxzt). With `quintKonnect { readSpecIr.set(true) }`,
`./gradlew -p example test --tests '*quinttest.CounterDriver*' -Pquint.replay=<file>` fails in
`compileTestKotlin` with `Unresolved reference 'TictactoeSpec'`.

Cause: `wireQuintIr(skipKsp = replayOverride != null)` (gradle-plugin/.../QuintIrWiring.kt) skips
passing `quintkonnect.irDir` to KSP when `-Pquint.replay` is set. The processor option changes, so
`kspTestKotlin` reruns without IR and generates no `<Module>Spec` types, and any driver using them
stops compiling. So README.md's "Replaying a saved trace" (no quint needed) doesn't hold for a
`readSpecIr` project at all.

Needs a decision: keep passing the IR when readSpecIr is on (replay then needs quint for `quintIr`
unless the IR is already built and up to date), or reuse existing IR output without depending on
`quintIr`/`checkQuint`. Add a functionalTest covering replay + readSpecIr.
