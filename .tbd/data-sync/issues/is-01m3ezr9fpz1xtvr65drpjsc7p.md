---
type: is
id: is-01m3ezr9fpz1xtvr65drpjsc7p
title: Prefix ITF decode error paths with state./picks.<name>
kind: task
status: closed
priority: 3
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T13:51:24.661Z
updated_at: 2026-09-26T14:51:05.821Z
closed_at: 2026-09-26T14:51:05.821Z
close_reason: state./picks.<name>. prefix landed on state-diagnostics (commit qyw) via a @PublishedApi internal prefixDecodeError helper in State.kt. checkKotlinAbi now fails on that helper's ABI line only; flagged to coordinator for merge, core/api/core.api left untouched.
resolution: null
duplicate_of: null
---
Since qk-ze9b, ITF decode errors name the field path relative to the decoded value (e.g. 'board.(1, 2): expected a string, got an int (5)'). The public ItfValue.decode(deserializer) has no root-name parameter, so core should add the root: TypedState.check wraps errors with 'state.', NondetPicks.decode/decodeOrNull with 'picks.<name>.' (e.g. catch SerializationException and rethrow with the prefixed message and the original as cause, or add an internal/@PublishedApi decode overload in :itf taking a root path; the latter changes the :itf ABI dump). Related: qk-w3h5 (real diff on state mismatch).
