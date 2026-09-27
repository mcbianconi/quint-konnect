---
type: is
id: is-01m3dzgwztnpzvh73fftntn07e
title: Upgrade to JUnit 6
kind: chore
status: closed
priority: 3
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsw93v96g3zbdqzjze2j
created_at: 2026-09-26T04:28:08.057Z
updated_at: 2026-09-27T13:22:12.954Z
closed_at: 2026-09-27T13:22:12.950Z
close_reason: "JUnit 5.14.4 -> 6.1.3 (latest stable). No source changes needed: KSP-generated tests only use @TestFactory/DynamicTest.dynamicTest, stable across 5.x/6.x. gradle-plugin still adds no JUnit dep. 388/388 tests pass, uniform 6.1.3 on every module's testRuntimeClasspath. quint stays 0.32.0 (still latest). Commit nkm on branch junit6."
resolution: null
duplicate_of: null
---
JUnit 5.12.2 -> 6.1.3, major version; follow migration notes. Also track quint CLI flag changes.
