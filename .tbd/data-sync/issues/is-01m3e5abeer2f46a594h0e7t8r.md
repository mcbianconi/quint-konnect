---
type: is
id: is-01m3e5abeer2f46a594h0e7t8r
title: ReplayListener interface and injectable Runner
kind: feature
status: open
priority: 2
version: 2
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3dzgvm5hzj70dt0qf53ag24
parent_id: is-01m3e5a9sj92e7ptv02j419tm8
created_at: 2026-09-26T06:09:24.942Z
updated_at: 2026-09-26T06:49:24.733Z
---
Runner, TraceGenerator and Logger are objects; Logger reads QUINT_VERBOSE at class init. Add a ReplayListener (trace start, step, mismatch, done) and a Runner instance taking config, generator and listener. Console logger becomes one listener; avoids interleaved output with parallel traces (qk-t281).
