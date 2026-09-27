# quint-konnect examples

A standalone Gradle build that applies `io.github.mcbianconi.quint-konnect` the way README.md's
"Quick start" does, with `readSpecIr` on so drivers use the generated `<Module>Spec` types.
`settings.gradle.kts` swaps the Maven Central coordinates for this repository's sources; nothing
else in the build is specific to living in this repository.

Run it from the repository root (needs `quint` in `PATH`):

```bash
./gradlew -p example build
QUINT_SEED=0x1234 QUINT_VERBOSE=1 ./gradlew -p example test --tests '*TicTacToeDriver*'
./gradlew -p example test -Pquint.maxSamples=1000   # override @QuintRun's maxSamples, no recompile
```

| Package | Shows |
|---|---|
| `tictactoe/` | `@QuintRun` driver, `TypedState` over the generated `TictactoeSpec` |
| `rockpaperscissors/` | nondet picks decoded into sum-type parameters |
| `buggy/` | a buggy implementation the test expects to fail |
| `partialstate/`, `projection/` | comparing part of the state with `@QuintIgnore` and `compareField` |
| `sumtypes/` | `DriverConfig(statePath = …, nondetPath = …)` |
| `quinttest/` | `@QuintTest` with `nondetPath` |
| `suspending/`, `asyncstore/` | `suspend` `@QuintAction`s |
| `invariants/` | `@QuintRun(invariants = [...])` |
