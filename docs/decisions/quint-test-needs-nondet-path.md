---
name: quint-test-needs-nondet-path
date: 2026-09-26
---

`@QuintTest` only replays a `quint test` trace when the spec records the action taken
in its own sum-type variable and the driver's `config()` points `DriverConfig.nondetPath`
at it. It does not work with the default `DriverConfig()` (bead qk-cxf6).

**Why:** `quint test` (0.32.0, confirmed via `quint test --help`) has no `--mbt` flag, so
it never writes the `mbt::actionTaken` / `mbt::nondetPicks` variables `Step.fromState`
falls back to by default. `quint run` has `--mbt`; `quint test` does not, and there is no
open issue or newer release changing that. Upstream `quint-connect` (Rust) has the same
`quint test` command construction (no `--mbt`) and documents the same fallback: a
`Config.nondet` path to a manually-modeled sum type, used "when tracking nondeterminism
manually rather than using Quint's builtin variables". That fallback is what
`Step.fromState`'s `extractFromSumType` branch already implements, gated on
`DriverConfig.nondetPath`.

A spec verified this end to end: a `type Action = Init | Add({ n: int })` variable set
alongside the real state in every action, with `config() = DriverConfig(nondetPath =
listOf("lastAction"))` on the driver, replays correctly via `@QuintTest` (see
`example/src/test/resources/quinttest/counter.qnt` and
`example/.../quinttest/CounterDriver.kt`). `ignoreUnknownKeys` on `QuintJson` means the
leftover `lastAction` field in state doesn't break `TypedState` comparison.

**How to apply:** Don't treat `@QuintTest`'s default config as usable out of the box;
every spec used with `@QuintTest` needs its own action-tracking sum-type variable, and
the driver must set `nondetPath` to it. Keep README.md/AGENTS.md's `@QuintTest`
documentation pointing this out instead of describing it as broken. If a future `quint`
release adds `--mbt` to `test` (check `quint test --help` when bumping the version pin
in `.github/workflows/ci.yml`), `TestConfig.toCommand` can add it and drop this
requirement.
