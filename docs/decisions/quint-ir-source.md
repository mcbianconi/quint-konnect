---
name: quint-ir-source
date: 2026-09-27
---

`quintIr` (gradle-plugin's `QuintIrTask`) runs `quint typecheck --out <file> <spec>`, not
`quint compile --target json`, to produce the JSON that ksp's IR parser (`ir/QuintIr.kt`) reads.
Both emit the same `{stage, modules, table, types, effects, errors}` shape (`compile`'s
`"compiling"` stage adds only a `main` module-id field on top of `typecheck`'s `"typechecking"`
stage); `types`/`table` carry every type this project needs (action/nondet types, state variable
types, record/sum typedefs).

**Why:** `quintIr` runs once per `.qnt` file, before KSP has read any `@QuintRun`/`@QuintTest`
annotation, so it doesn't know a driver's `main`/`init`/`step` overrides yet (qk-8i6m's design
deliberately keeps IR generation and driver scanning separate: one Gradle task per spec file,
looked up by KSP per driver afterward). `quint compile` needs `--init`/`--step` to resolve to real
actions (defaulting to `"init"`/`"step"`) and would fail on a spec using different names via
`@QuintRun(init = ..., step = ...)`, or on a library-only module with no entry point at all.
`quint typecheck` needs none of that and still exits 0 with `--out` written even when the file
declares multiple modules or none of them look like an entry point.

Both `--out` invocations write the file even on a typecheck error and exit 1 with **no other
diagnostic**: the only error text is the written file's own top-level `errors[].explanation`
array (confirmed against quint 0.32.0). `QuintIrTask` reads that array into the `GradleException`
message and deletes the partial output on failure.

**How to apply:**
- Don't switch to `compile` to get its `main` field; it doesn't cover an IR generation task that
  runs before any driver's `main`/`init`/`step` are known.
- ksp's `loadQuintIrModule(irDir, specPath, main)` picks the module by name when
  `@QuintRun`/`@QuintTest`'s `main` is set, otherwise takes the sole module, otherwise falls back
  to the last module in the file (a guess, not derived from quint's own filename-to-module-name
  convention: `RunConfig`/`TestConfig` don't reimplement that either, they just omit `--main` and
  let `quint run`/`quint test` compute it). Revisit if a real multi-module spec needs something
  more precise.
- An action's `nondetParams` are collected transitively: not just `nondet` bindings written
  directly in that action's own body, but also ones bound inside any other top-level `def`/`val`/
  `action` it reaches by name or application (resolved through the IR's own `table` map), since
  that's what `mbt::nondetPicks` actually populates at runtime (see tictactoe.qnt's `MoveX`,
  which has no `nondet` of its own but reaches `Win`/`Block`/`StartInCorner`/`SetupWin`/
  `MoveToEmpty`'s nondets through `if`-branches and zero-arg name references).
