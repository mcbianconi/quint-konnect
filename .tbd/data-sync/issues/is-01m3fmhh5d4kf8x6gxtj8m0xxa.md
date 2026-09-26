---
type: is
id: is-01m3fmhh5d4kf8x6gxtj8m0xxa
title: TypedState.compareField never receives "<root>" as documented
kind: bug
status: closed
priority: 2
version: 2
labels: []
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T19:54:43.244Z
updated_at: 2026-09-26T19:55:47.796Z
closed_at: 2026-09-26T19:55:47.795Z
close_reason: compareField now receives "<root>" for the whole state (branch compare-field-root).
resolution: null
duplicate_of: null
---
State.kt's KDoc for TypedState.compareField says the path is "dot/bracket-joined the way this
class's diff messages name it, e.g. \"count\", \"cells.(1, 2)\", \"items[0]\", or \"<root>\" for
the whole state" — implying an override that checks path == "<root>" handles the whole-state case.

But StateDiff.kt's buildFieldDiff calls diffValues("", tree, tree, compareField, out), and
diffValues calls compareField(path, ...) with the raw path directly; path.orRoot() is only applied
when building the *message text* for out (a `String` describing a mismatch), never when calling
compareField. So compareField receives "" for the whole-state case, not "<root>": an override
written to match "<root>" (as the KDoc suggests) silently never fires.

Repro: any TypedState subclass with
  override fun compareField(path: String, spec: String, impl: String): Boolean? =
      if (path == "<root>") true else null
never short-circuits — buildFieldDiff still recurses into the full structural comparison.

Found while writing example/.../projection/WarehouseState.kt for qk-pdcy (realistic examples).

Fix: either pass path.orRoot() into compareField (breaking change, "" no longer valid), or fix the
KDoc to document "" as the whole-state path instead of "<root>".
