---
type: is
id: is-01m3jjwb23wdrkmjv4m93y13kh
title: Driver with both @QuintRun and @QuintTest generates colliding files
kind: bug
status: closed
priority: 3
version: 3
labels: []
dependencies: []
created_at: 2026-09-27T23:23:23.584Z
updated_at: 2026-09-27T23:40:39.148Z
closed_at: 2026-09-27T23:40:39.147Z
close_reason: Processor rejects a driver with both annotations; stacked on qk-97b8
resolution: null
duplicate_of: null
---
QuintKonnectProcessor processes @QuintRun and @QuintTest separately. A driver with both annotations runs StepMethodGenerator twice (before qk-97b8 too) and, after qk-97b8, writes <Driver>QuintSuite twice, which is a KSP file collision. DriverManifestWriter already handles this case with a -<kind> suffix. Either reject both annotations on one driver with a compile error, or name the suite per kind. Found during qk-97b8 review.
