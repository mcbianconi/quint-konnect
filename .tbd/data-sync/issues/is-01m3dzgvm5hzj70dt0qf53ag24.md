---
type: is
id: is-01m3dzgvm5hzj70dt0qf53ag24
title: Disable ANSI colours when output is not a terminal
kind: task
status: closed
priority: 3
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T04:28:06.660Z
updated_at: 2026-09-26T14:00:56.957Z
closed_at: 2026-09-26T14:00:56.957Z
close_reason: "ConsoleReplayListener now gates ANSI codes on resolveUseColor: System.console() != null by default, NO_COLOR (any value) disables, QUINT_COLOR=always|never overrides. Public constructors unchanged (checkKotlinAbi passes); internal 3-arg constructor added for tests. Commit qrm on branch itf-dx."
resolution: null
duplicate_of: null
---
Logger always emits ANSI codes, cluttering CI logs.
