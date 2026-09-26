---
name: runner-failure-contract
date: 2026-09-26
---

`Runner` wraps any `Throwable` raised while replaying a step (step extraction, driver
action, state check) in an `AssertionError` whose message names the trace, step, action
and nondet picks, with the original throwable as `cause`. The reproduce-seed line
(`QUINT_SEED=...`) is printed for every failure, including `Error` subclasses.

**Why:** JUnit and kotlin.test assertion failures are `AssertionError`, which the old
`catch (e: Exception)` skipped, hiding the seed. Without the location, users could not
tell which step diverged.

**How to apply:** Tests that expect a replay failure assert `AssertionError` (see
`example/.../buggy/BuggyRockPaperScissorsTest.kt`) and inspect `cause` for the original
type. Keep the wrapping at the step level; don't add another wrapping layer elsewhere.
