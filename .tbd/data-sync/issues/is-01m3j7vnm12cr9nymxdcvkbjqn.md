---
type: is
id: is-01m3j7vnm12cr9nymxdcvkbjqn
title: Use the generated <Module>Spec types in every example driver where they fit
kind: task
status: closed
priority: 3
version: 4
delegate: claude-code@vm
labels:
  - roadmap
dependencies: []
parent_id: is-01m3j7vmxf3e87tr20a324y2s9
hold: null
hold_until: null
created_at: 2026-09-27T20:10:47.296Z
updated_at: 2026-09-27T20:42:12.983Z
started_at: 2026-09-27T20:40:26.066Z
closed_at: 2026-09-27T20:42:12.983Z
close_reason: Done in 5bd0d3b on claude/example-standalone-qk-03a3
resolution: null
duplicate_of: null
---
Only `tictactoe/TicTacToeState.kt` uses them today. Per driver (paths under
`example/src/test/kotlin/io/github/mcbianconi/quintkonnect/example/`):

| Driver | Target |
|---|---|
| `rockpaperscissors/` | `TypedState<..., RockPaperScissorsSpec.State>`; `decideMoves(move1: RockPaperScissorsSpec.Move, ...)`; delete `MoveSer`/`GameStatusSer`/`PlayerSer`/`RpsState` from `RpsGameState.kt`. |
| `buggy/` | Not `@QuintRun`, so no Spec is generated in its package: import `rockpaperscissors.RockPaperScissorsSpec` (it already imports the hand-written types from there). |
| `partialstate/` | Keep the hand-written `PartialRpsState` (needs `@QuintIgnore`, which can't go on generated classes) but type its fields with `RockPaperScissorsSpec.Move`/`GameStatus`. One-line comment saying why. |
| `sumtypes/` | `statePath = machine` → `TypedState<..., VendingmachineSpec.Machine>`; delete `VendingMachineValue`. |
| `asyncstore/`, `invariants/` | `BankledgerSpec.State`, `CounterSpec.State`; delete `BankLedgerValue`, `InvariantCounterValue`. |
| `projection/` | Keep `WarehouseValue` (it shows `@QuintIgnore` projection); add a one-line comment why it isn't `WarehouseSpec.State`. |
| `quinttest/`, `suspending/` | `CounterSpec.State` includes `lastAction` (the `nondetPath` carrier). Keep the hand-written class with a comment pointing to qk-ymex, unless tracking `lastAction` in the driver reads naturally. |

Acceptance: `./gradlew -p example build` passes; no remaining hand-written `@Serializable` class
in `example/` duplicates a generated one; each remaining hand-written state class has a
one-line reason. Update README/skill snippets that show the old hand-written RPS types.
