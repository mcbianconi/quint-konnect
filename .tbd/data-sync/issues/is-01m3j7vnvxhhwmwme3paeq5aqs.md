---
type: is
id: is-01m3j7vnvxhhwmwme3paeq5aqs
title: Let a driver project the generated <Module>Spec.State
kind: feature
status: open
priority: 4
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3j7vmxf3e87tr20a324y2s9
created_at: 2026-09-27T20:10:47.548Z
updated_at: 2026-09-27T20:10:47.548Z
---
The generated `State` has every state variable, so it can't be used when a driver compares a
subset: `@QuintIgnore` fields (example `projection/`, `partialstate/`), the `nondetPath` carrier
variable in `@QuintTest` specs (`lastAction` in `quinttest/counter.qnt`, see
docs/decisions/quint-test-needs-nondet-path.md), or variables the implementation doesn't model.
Today the driver falls back to a hand-written class (which works: `CounterValue(count)` passes),
so the gap is lost codegen, not a decode failure. KSP can't see `Driver.config()` (runtime), so
the design needs a compile-time signal (e.g. an annotation parameter listing ignored variables,
or generating `@QuintIgnore`-able variants). Decide the design in the bead before implementing;
the CLAUDE.md skill-sync rule applies if annotations change.
