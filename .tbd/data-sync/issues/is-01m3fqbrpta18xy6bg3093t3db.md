---
type: is
id: is-01m3fqbrpta18xy6bg3093t3db
title: Seed-reproduction line omits -Pquint.maxSamples/maxSteps overrides
kind: bug
status: closed
priority: 2
version: 4
labels: []
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T20:44:00.089Z
updated_at: 2026-09-27T13:41:02.401Z
closed_at: 2026-09-27T13:41:02.400Z
close_reason: "Fixed: ConsoleReplayListener.reproduceCommand() now includes -Pquint.maxSteps/-Pquint.maxSamples overrides alongside QUINT_SEED"
resolution: null
duplicate_of: null
---
ConsoleReplayListener prints "Reproduce this error with `QUINT_SEED=...`" (core/src/main/kotlin/io/github/mcbianconi/quintkonnect/listener/ConsoleReplayListener.kt:115,123) but never includes the maxSamples/maxSteps in effect for that run. If a user overrode either with -Pquint.maxSamples/-Pquint.maxSteps, copying just QUINT_SEED from the reproduce line and rerunning without repeating that override risks not reaching the state that triggered the original failure -- silently 'un-reproducing' the bug (whether the seed alone reproduces at the annotation's default maxSteps was not itself tested; what was confirmed is that reproduction worked when the same -Pquint.maxSteps=30 override was repeated, and that the printed line omits it).

Found while eval'ing the quint-konnect skill on a scratch project (bead qk-graf): a clamp bug only surfaced after raising -Pquint.maxSteps to 30, and reproduction was done by repeating that same override alongside QUINT_SEED. Fix: have ConsoleReplayListener also print the maxSamples/maxSteps (and any other non-default run config) alongside QUINT_SEED, so a copy-pasted reproduce command is actually complete.
