---
type: is
id: is-01m3e5abxs1zdjeqttjjw6n3e7
title: Runner-neutral suite with JUnit adapter
kind: feature
status: closed
priority: 4
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3e5a9sj92e7ptv02j419tm8
created_at: 2026-09-26T06:09:25.433Z
updated_at: 2026-09-27T20:44:26.794Z
closed_at: 2026-09-27T20:44:26.793Z
close_reason: Generated a runner-neutral <Driver>QuintSuite object per @QuintRun/@QuintTest driver (core's new QuintSuite interface) and made the generated JUnit class a thin adapter that delegates to it; added the quintkonnect.adapter KSP option (junit default / none) so a driver can skip the JUnit class entirely. Commit d7ede46.
resolution: null
duplicate_of: null
---
Generated tests are JUnit 5 only. Generate a runner-neutral suite description plus a thin JUnit adapter; Kotest adapter later.
