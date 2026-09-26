---
type: is
id: is-01m3dxx05zr5y1wbb001z8qbq0
title: Catch up with quint-connect (Rust) upstream changes since port started
kind: task
status: closed
priority: 2
version: 3
delegate: claude-code@macmurillo.local
labels:
  - upstream-sync
dependencies: []
hold: null
hold_until: null
created_at: 2026-09-26T03:59:47.390Z
updated_at: 2026-09-26T04:09:40.783Z
started_at: 2026-09-26T04:05:49.438Z
closed_at: 2026-09-26T04:09:40.782Z
close_reason: "Upstream caught up through 4f018f54 (v0.1.2). Applied the equivalent Windows fix from #11 (998d7cfc): quint-konnect now invokes quint.cmd on Windows since ProcessBuilder doesn't resolve PATHEXT. Committed on branch fix/windows-quint-cmd (not pushed). Note: unverified on real Windows - args like --match \"^name$\" pass through cmd.exe when launching a .cmd shim, where ^ is an escape char, so quoting may need a follow-up if it breaks in practice."
resolution: null
duplicate_of: null
---
The Kotlin port (quint-konnect) was started 2026-02-23 from quint-co/quint-connect (Rust),
formerly informalsystems/quint-connect: https://github.com/quint-co/quint-connect

Upstream commits since the port started (as of 2026-09-26):

- 998d7cfc "fix: windows compatibility for running quint tests (#11)" (2026-05-25)
- 4f018f54 "Release v0.1.2 (#12)" (2026-05-25)

Changelog entry:

## [0.1.2] - 2026-05-25
### Fixed
- Compatibility with running Quint on Windows (#11)

TODO: review the Windows-compat fix (#11) and determine whether quint-konnect's
CLI invocation / process handling needs an equivalent fix for Windows, then close
this out.

Baseline recorded for the upstream-watch routine: 4f018f54 (v0.1.2, 2026-05-25).
