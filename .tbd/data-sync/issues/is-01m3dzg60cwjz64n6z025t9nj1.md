---
type: is
id: is-01m3dzg60cwjz64n6z025t9nj1
title: Escape regex in --match and string literals in KSP-generated code
kind: bug
status: closed
priority: 3
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:44.523Z
updated_at: 2026-09-26T05:42:29.041Z
closed_at: 2026-09-26T05:42:29.036Z
close_reason: Escaped --match regex in TestConfig.kt (ECMAScript SyntaxCharacters) and added String.kotlinStringLiteral() helper used by all three KSP generators (QuintRunTestGenerator, QuintTestTestGenerator, StepMethodGenerator) for spec/seed/test/main/init/step/action-name literals. Added TestConfigTest.kt and an example/escaping/ fixture proving generated code compiles and runs. Commit nzu on branch escape-generated-literals.
resolution: null
duplicate_of: null
---
trace/TestConfig.kt:20 builds ^test$ without escaping. KSP generators paste spec, seed and action names into Kotlin string literals without escaping, so $ or quotes break compilation.
