---
type: is
id: is-01m3dzgs4n314gkbw5qhsbtqhw
title: "Build cleanup: convention plugin and version catalog fixes"
kind: chore
status: in_progress
priority: 1
version: 4
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3dzh39fs71vn26cngefs9y4
  - type: blocks
    target: is-01m3dzh3h36exkjke6fkw32cph
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T04:28:04.116Z
updated_at: 2026-09-26T06:28:02.251Z
---
group/version/jvmToolchain are duplicated in 4 modules. kotlinx-coroutines catalog entry uses the Kotlin version and is unused; example hardcodes unused coroutines-core 1.10.2; example main source set does not need :core.
