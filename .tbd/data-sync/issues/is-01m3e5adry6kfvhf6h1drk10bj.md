---
type: is
id: is-01m3e5adry6kfvhf6h1drk10bj
title: Remove the generatedStep boilerplate from drivers
kind: feature
status: closed
priority: 3
version: 4
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e2j18mpf39j7gr2442mrsr
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T06:09:27.325Z
updated_at: 2026-09-26T14:48:17.346Z
closed_at: 2026-09-26T14:48:17.345Z
close_reason: "Implemented: Driver.step default reflective dispatch to KSP-generated <pkg>.<Driver>StepsKt.generatedStep via ClassValue cache, InvocationTargetException unwrapped; example drivers, README/AGENTS.md, core.api updated; core+ksp tests added"
resolution: null
duplicate_of: null
---
Drivers must write override fun step(step: Step) = generatedStep(step), which is red in a fresh clone before KSP runs and easy to forget. Generate a base class or delegate instead.
