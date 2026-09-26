---
type: is
id: is-01m3dxx05zr5y1wbb001z8qbq0
title: Catch up with quint-connect (Rust) upstream changes since port started
kind: task
status: open
priority: 2
version: 1
labels:
  - upstream-sync
dependencies: []
created_at: 2026-09-26T03:59:47.390Z
updated_at: 2026-09-26T03:59:47.390Z
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
