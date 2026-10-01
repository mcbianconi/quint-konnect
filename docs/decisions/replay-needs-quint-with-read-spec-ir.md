---
name: replay-needs-quint-with-read-spec-ir
date: 2026-09-27
---

`-Pquint.replay` skips `checkQuint`/`downloadQuint` on the Test task itself (README.md's
"Replay a saved trace"), but `QuintIrWiring.kt`'s `wireQuintIr` wires `quintIr` into every KSP
task whenever `quintKonnect { readSpecIr.set(true) }`, regardless of `-Pquint.replay`. A replay run
with `readSpecIr` on still needs `quint` installed (bead qk-sa74).

**Why:** KSP's generated `<Module>Spec` types (README.md's "Let KSP generate the spec types") are
a compile-time dependency of the driver code, not something a replay run can skip the way it skips
actually invoking `quint` at test time. An earlier version of `wireQuintIr` took a `skipKsp` flag
set from `replayOverride != null` and left the KSP processor option unset on a replay build. That
didn't recover the previous, already-generated types: KSP treats its processor options as a build
input, so the option changing at all (from a path to unset) left `kspTestKotlin` UP-TO-DATE with a
different option value, reran the processor with no IR to read, and generated no `<Module>Spec`
types — breaking compilation for any driver using them, even one unrelated to the `--tests` filter
of the replay run.

**How to apply:**
- Don't reintroduce a flag that skips wiring `quintIr` into KSP based on `-Pquint.replay`. The
  wiring must stay identical whether or not `-Pquint.replay` is set; only the Test task's own
  `checkQuint`/`downloadQuint`/`generateQuintTraces` dependencies (guarded by `replayOverride ==
  null` in `QuintKonnectPlugin.kt`) are replay-specific.
- `quintIr` (`@CacheableTask`) is still Gradle's normal incremental build: if its inputs (the spec
  files) haven't changed since the last build, it's UP-TO-DATE and doesn't rerun `quint typecheck`.
  But `checkQuint` has no declared outputs (`CheckQuintTask`'s own comment: "never up-to-date"), so
  it always runs `quint --version` when it's part of the graph — meaning a `readSpecIr` project
  needs `quint` on PATH for every build, replay or not, not just the first one.
