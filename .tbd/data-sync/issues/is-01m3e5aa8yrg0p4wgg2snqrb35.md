---
type: is
id: is-01m3e5aa8yrg0p4wgg2snqrb35
title: Read quint typed IR at build time
kind: task
status: closed
priority: 3
version: 5
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e5aag6b4dd6gnkz2jn871x
  - type: blocks
    target: is-01m3e5aaqvhketzpn1h4662zt3
parent_id: is-01m3e5aa1sevc19rem5gdd0hye
created_at: 2026-09-26T06:09:23.741Z
updated_at: 2026-09-27T14:05:00.685Z
closed_at: 2026-09-27T14:05:00.685Z
close_reason: "Added gradle-plugin's quintIr task (quint typecheck --out per spec, wired into KSP as the quintkonnect.irDir option, skipped under -Pquint.replay) and ksp's IR model+parser (ir/QuintIr.kt: actions with transitively-collected nondet params and types, state variable types, record/sum typedefs). QuintKonnectProcessor loads the IR for a driver's spec when the option is present; qk-75ad/qk-ixox can build on QuintModuleIr/loadQuintIrModule next. Decision recorded in docs/decisions/quint-ir-source.md."
resolution: null
duplicate_of: null
---
Run quint compile --target json (or typecheck --out) at build time and expose action names, nondet names and types to KSP. Available in quint 0.32.0.
