---
name: generate-quint-traces-task
date: 2026-09-27
---

`generateQuintTraces` (gradle-plugin's `GenerateQuintTracesTask`) runs `quint` once per
`@QuintRun`/`@QuintTest` driver into `build/quint-konnect/traces/<Driver>/`, reading a small JSON
manifest KSP writes per driver instead of the SOURCE-retention annotation itself (a Gradle task
can't read that). Every Test task depends on it (unless `-Pquint.replay` is set) and gets
`quintkonnect.tracesDir` pointed at its output; `ReplayRunner`'s default `TraceSource`
(`TracesDirTraceSource`, core's `trace/TraceSource.kt`) replays `<tracesDir>/<testName>/` when
present, falling back to invoking `quint` itself (`TraceGenerator`) otherwise — so `integration-tests` and any
non-plugin consumer keep working unchanged (qk-adm2).

**Manifest:** KSP (`ksp/.../generators/DriverManifestWriter.kt`) writes one JSON resource per driver
at `build/generated/ksp/<target>/test/resources/quintkonnect/traces-manifest/<package path>/
<Driver>-<kind>.json` (`kind` is `run` or `test`; KSP rejects a driver with both `@QuintRun` and
`@QuintTest`). It carries `driver` (FQN), `kind`,
`spec`, `main`/`init`/`step`/`test`/`maxSamples`/`maxSteps`/`seed` (whichever apply, omitted when
blank) and `invariants`. `generateQuintTraces` globs `build/generated/ksp/**/quintkonnect/
traces-manifest/**/*.json` for its `manifests` input (not a Gradle-computed exact KSP resource
path, which differs across KSP/Kotlin versions and target names — see "Problems" in the
implementing agent's report) and depends on `kspTestKotlin` by name (mirrors `wireQuintIr`'s own
reasoning for not depending on KSP's internal task type).

**Spec files are task inputs (qk-q5z8):** a manifest holds the spec *path*, not its content, so
the `specs` `@InputFiles` (`RELATIVE` path sensitivity) carries the content: `quintKonnect.quintIrSpecs`
(default `src/test/resources/**/*.qnt`, which also covers imported files there) plus each manifest's
resolved `spec`, read lazily from the `manifests` elements after `kspTestKotlin` ran. Without it, a
spec change with a pinned seed left the task UP-TO-DATE/FROM-CACHE and replayed stale traces (the
build cache could also hand them to other machines).

**Output directory is keyed by the driver's simple class name** (`DriverManifest.simpleName`),
matching `ReplayRunner`'s existing `testName` (the driver's simple name, unchanged by this work).
Two same-named drivers in different packages collide on this directory the same way they already
collide on saved-failure file names and `replayCommand`'s `--tests '*testName*'` guess (see that
function's own doc comment in `core/.../trace/FailureTraceWriter.kt`); this wasn't made worse or
better here. A driver's own trace directory also carries `seed.txt` (the seed `quint` actually ran
with) and, on a non-zero `quint` exit, `error.txt` instead of trace files.

**Why `quint` failures don't fail the task:** an invariant violation, or any other non-zero
`quint` exit, is today a normal `@QuintRun`/`@QuintTest` test failure (surfaced by `TraceGenerator`
at test time, inside the forked Test JVM). If `generateQuintTraces` threw on the same condition, it
would fail the whole build before any test ran, for every driver, not just the one whose spec/
invariant is broken. Instead it records the exit and a short message (mirroring
`TraceGenerator`'s own violation-message building, simplified — no violating-trace dump, since this
task doesn't have `:core`/`:itf` on its classpath to parse ITF) as `error.txt`, and
`TracesDirTraceSource.generate()` throws with that text at replay time, restoring the original
per-driver test failure.

**Why the seed can't just be `genSeed()`'s random fallback:** `genSeed()` (core's
`trace/Seed.kt`) picks a fresh random seed at *test* time whenever nothing else pins it (no
`-Pquint.seed`, no `QUINT_SEED`, a blank annotation `seed`) — a new value on every JVM run, by
design, for exploration. A Gradle task's cache key is its declared `@Input`s; if the seed used to
invoke `quint` were re-randomized at every *configuration*, the task could never be UP-TO-DATE or
cache-hit even with nothing else changed. `GenerateQuintTracesTask` instead:

- Resolves each driver's seed with the same precedence as `genSeed()` (an explicit
  `-Pquint.seed`-style override first, then the driver's own manifest `seed`, then `QUINT_SEED`,
  then a random fallback) but only inside `@TaskAction` (`run()`), i.e. only when the task actually
  executes — never inside `upToDateWhen`/configuration, so a cache hit or UP-TO-DATE result never
  triggers a fresh random draw.
- Only enables `UP_TO_DATE`/build-cache (`outputs.upToDateWhen` / `outputs.cacheIf`) when *every*
  driver's seed is pinned by something other than the random fallback (checked by reading the
  already-resolved `@InputFiles` manifests, not by trying to predict the random draw). An unpinned
  driver's seed genuinely can't be reproduced from the declared inputs, so its build is never
  reported as cacheable — matching today's "different traces every run" behavior for that driver
  exactly, just at task-run granularity instead of per-JVM.
- `-Pquint.seed` and `QUINT_SEED` are read once at Gradle configuration time (`seedOverride`,
  `envSeed` — both `@Input`), same as the existing `-Pquint.maxSamples`/`-Pquint.maxSteps`
  precedent (`QuintKonnectPlugin.kt`): a value read at configuration time can't itself be
  "unpinned", so setting either one always makes a build cacheable regardless of individual
  manifests' own `seed`.

**How to apply:** Don't add an opt-out flag for this task (the bead's intent is default-on: every
Test task depends on it unless replaying, matching `checkQuint`/`downloadQuint`'s own guard). Don't
call `RunConfig`/`TestConfig.toCommand` from the task action — those read `quintkonnect.*` system
properties and resolve a relative `spec` against the *process'* own state, which in the Gradle
daemon is the daemon's, not this task's; `GenerateQuintTracesTask` mirrors the argv construction
directly instead (kept in step by hand if `RunConfig`/`TestConfig` change — no test currently pins
byte-for-byte parity between the two, see "Problems"). Shrinking failing traces (qk-a8ay) is a
separate task, per the parent bead.
