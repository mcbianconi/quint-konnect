---
name: itf-module
date: 2026-09-26
---

ITF parsing and normalization (`ItfValue`, `ItfTrace`/`parseTrace`, `ItfValueSerializer`,
`ItfValue.decode`, `BigIntegerSerializer`) lives in its own Gradle module `:itf`, package
`io.github.mcbianconi.itf`, artifact `io.github.mcbianconi:itf-kotlin` (same repo, version and
release as quint-konnect). `core` depends on it with `api(project(":itf"))` since `ItfValue` is
part of core's public API (`State.check`, `NondetPicks.get`, `Step.state`).

**Why:** Mirrors upstream `quint-connect` (Rust), which depends on the standalone `itf-rs` crate
rather than folding ITF handling into its main crate. A separate module is also reusable by other
JVM ITF producers besides Quint, such as Apalache. Splitting it out before the first publish
(qk-j02b) avoids a breaking change later.

**How to apply:** Add new ITF parsing/normalization code to `:itf`, not `:core`. Keep imports as
`io.github.mcbianconi.itf.*`; don't reintroduce `io.github.mcbianconi.quintkonnect.itf`.
