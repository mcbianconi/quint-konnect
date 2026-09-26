---
name: no-windows-support
date: 2026-09-26
---

quint-konnect does not target Windows.

**Why:** Explicit call by the project owner — Windows compatibility is not a
priority for this project.

**How to apply:** Don't add Windows-specific compatibility code (e.g. `.cmd` shim
handling, path-separator workarounds, PATHEXT resolution) or treat Windows platform
gaps as bugs to fix. Existing Windows-specific code (e.g. `quintCommand()` in
`core/src/main/kotlin/io/github/mcbianconi/quintkonnect/trace/GeneratorConfig.kt`,
which picks `quint.cmd` on Windows) is not a priority to maintain, extend, or test
further — leave it as-is unless asked otherwise.
