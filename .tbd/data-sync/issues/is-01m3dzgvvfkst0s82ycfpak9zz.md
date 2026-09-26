---
type: is
id: is-01m3dzgvvfkst0s82ycfpak9zz
title: Typed Driver<S> instead of State<*>
kind: feature
status: closed
priority: 3
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T04:28:06.894Z
updated_at: 2026-09-26T15:08:33.920Z
closed_at: 2026-09-26T15:08:33.919Z
close_reason: "Committed oyv on typed-driver: KSP validates quintState() return type against the driver class via State<in X> + isAssignableFrom."
resolution: null
duplicate_of: null
---
quintState(): State<*> plus unchecked cast in Runner.kt:36 defers mismatch detection to runtime.
