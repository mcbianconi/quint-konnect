---
name: quint-version-pin
date: 2026-09-26
---

CI installs a pinned quint version (`@informalsystems/quint@0.32.0`), the same version
used locally.

**Why:** The library builds `quint run` / `quint test` command lines with specific flags
(`--mbt`, `--n-traces`, `--out-itf`, `--match`) and parses ITF output; an unpinned
quint could change either and break CI without a code change.

**How to apply:** Bump the pin deliberately, checking the flags in
`core/.../trace/RunConfig.kt` and `TestConfig.kt` against `quint run --help` and
`quint test --help` for the new version.
