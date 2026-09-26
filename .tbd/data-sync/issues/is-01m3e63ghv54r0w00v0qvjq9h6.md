---
type: is
id: is-01m3e63ghv54r0w00v0qvjq9h6
title: RunConfig's run_{seq}.itf.json isn't zero-padded, breaking trace order at 10+ samples
kind: bug
status: open
priority: 2
version: 1
labels: []
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T06:23:09.370Z
updated_at: 2026-09-26T06:23:09.370Z
---
RunConfig.toCommand passes --out-itf .../run_{seq}.itf.json to quint, and TraceGenerator.readTraces sorts files by name (File.listFiles()?.sortedBy { it.name }). Quint's {seq} placeholder isn't zero-padded, so for maxSamples/nTraces >= 10 the lexicographic file-name sort puts run_10.itf.json, run_11.itf.json, ... ahead of run_2..run_9, scrambling trace order relative to the actual run sequence. See TraceGeneratorTest.kt's disabled test `traces stay in numeric sequence order past 9 samples`, which reproduces this with a fake GeneratorConfig using the same naming scheme. Fix: either zero-pad the {seq} placeholder in RunConfig (if quint's CLI supports a width option or a fixed-width format), or have TraceGenerator sort numerically by extracting the sequence number instead of by raw file name.
