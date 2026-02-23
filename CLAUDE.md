# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Build & Test Commands

```bash
./gradlew :annotations:build         # Build annotation declarations
./gradlew :core:test                 # Run core unit tests (no quint CLI required)
./gradlew :ksp:build                 # Build KSP processor
./gradlew :example:build             # Build example + run end-to-end test (requires quint in PATH)
./gradlew build                      # Build all modules

QUINT_VERBOSE=1 ./gradlew :example:test   # Verbose: shows each trace and step
QUINT_VERBOSE=2 ./gradlew :example:test   # Very verbose: shows raw ITF state at each step
QUINT_SEED=0x1234 ./gradlew :example:test # Reproducible run with fixed seed
```

## Architecture

**quint-konnect** is a Kotlin port of [quint-connect](https://github.com/informalsystems/quint-connect). It bridges [Quint](https://github.com/informalsystems/quint) formal specifications with Kotlin implementations via model-based testing. The `quint` CLI generates ITF traces from a Quint spec; this library replays them against Kotlin code.

### Modules

- **`annotations/`** — Annotation declarations only (`@QuintAction`, `@QuintRun`, `@QuintTest`). No runtime dependency.
- **`core/`** — Runtime library: ITF parsing, trace generation, step extraction, state comparison, runner.
- **`ksp/`** — KSP2 processor. Reads `@QuintRun`/`@QuintTest` on driver classes and generates JUnit 5 test classes and `generatedStep()` extension functions.
- **`example/`** — TicTacToe end-to-end example.

### Data Flow

```
Quint Spec → [quint CLI subprocess] → ITF JSON files → parseTrace() → ItfTrace
                                                                          ↓
                                                                    Step.fromState()
                                                                     ↙        ↘
                                                              Driver.step()  State.check()
```

### Key Traits / Interfaces

- **`Driver`** (`core/src/main/kotlin/.../Driver.kt`) — Implement `step(Step)` to execute one trace step. Override `config()` for custom state/nondet paths. Override `quintState()` to return a `State<*>` for state checking.
- **`State<D>`** (`core/src/main/kotlin/.../State.kt`) — Interface with `check(driver, specValue)`. Use `State.disabled()` to skip state checking.
- **`TypedState<D, S>`** — Abstract class: implement `extractFromDriver(driver): S`, provide a `KSerializer<S>`. The framework deserializes the spec state via JSON round-trip and compares with `equals()`.

### KSP Code Generation

**`@QuintAction("ActionName")` on methods** → generates `ClassName.generatedStep(step: Step)`:
```kotlin
fun TicTacToeDriver.generatedStep(step: Step) {
    when (step.actionTaken) {
        "MoveX" -> {
            val coordinate = step.nondetPicks.decodeOrNull<List<Long>>("coordinate")
            this.moveX(coordinate)
        }
        else -> error("Unimplemented action: ${step.actionTaken}")
    }
}
```
- Nullable parameter type → `decodeOrNull` (optional pick)
- Non-nullable parameter type → `decode` (required pick, throws if missing)
- `name` defaults to the function name if omitted from the annotation

**`@QuintRun` / `@QuintTest` on driver classes** → generates a JUnit 5 test class that calls `Runner.runTest()`.

Driver classes and `@QuintRun`/`@QuintTest` annotations **must be in test sources** (`src/test/kotlin/`). Use `kspTest(project(":ksp"))` in `build.gradle.kts`.

### ITF Value Model (`core/src/main/kotlin/.../itf/`)

`ItfValue` is a sealed class with 10 variants mirroring Quint's type system:

| ITF JSON | ItfValue variant |
|----------|-----------------|
| `true` / `false` | `Bool` |
| `42` | `Num` |
| `"hello"` | `Str` |
| `{"#bigint": "123"}` | `BigInt` |
| `[1, 2]` | `List` |
| `{"#tup": [1, 2]}` | `Tup` |
| `{"#set": [1, 2]}` | `Set` |
| `{"#map": [[k,v],...]}` | `Map` |
| `{"field": ...}` | `Record` |

`toNormalizedJson()` converts `ItfValue` to plain `JsonElement` for standard kotlinx.serialization:
- `Tup` → `JsonArray` (so `List<T>` and `Pair<A,B>` deserialization works)
- `BigInt` → `JsonPrimitive(Long)` (so `Long`/`Int` deserialization works)
- `Map` with numeric keys → `JsonObject` with string keys (so `Map<Long, V>` works)

### Step Extraction (`core/src/main/kotlin/.../Step.kt`)

Two modes, controlled by `DriverConfig.nondetPath`:
- **MBT mode** (default, empty `nondetPath`): reads `mbt::actionTaken` and `mbt::nondetPicks`
- **Sum type mode**: follows `nondetPath` to a `{tag, value}` record

### Quint Option Unwrapping

`NondetPicks.fromItfValue()` automatically unwraps Quint's `Option` type:
- `{tag: "Some", value: v}` → keeps `v`
- `{tag: "None"}` → drops the pick (absent from the map)

## Environment Variables

- `QUINT_VERBOSE` — `0` (default), `1` (trace/step info), `2` (raw ITF state)
- `QUINT_SEED` — Fixed hex seed for reproducible runs (e.g. `0xdeadbeef`)

## Writing a Driver

```kotlin
// src/test/kotlin/...
@QuintRun(spec = "src/test/resources/my.qnt", maxSamples = 10)
class MyDriver : Driver {
    override fun step(step: Step) = generatedStep(step)  // wires in the generated dispatcher
    override fun quintState(): State<*> = MyState()

    @QuintAction("init")
    fun init() { /* reset state */ }

    @QuintAction("Move")
    fun move(position: List<Long>) { /* execute move */ }

    @QuintAction("Undo")
    fun undo(position: List<Long>?) { /* nullable = optional pick */ }
}
```

## Representing Quint Types in Kotlin

| Quint type | Kotlin `@Serializable` type |
|------------|----------------------------|
| `int` | `Long` |
| `bool` | `Boolean` |
| `str` | `String` |
| `(int, int)` tuple | `List<Long>` (index 0 = `._1`, index 1 = `._2`) |
| `int -> V` map | `Map<Long, V>` |
| `Set[T]` | `List<T>` |
| `type P = X \| O` sum type | `@Serializable @JsonClassDiscriminator("tag") sealed class` |
| `type S = Foo(P) \| Bar` | `sealed class` with `data class Foo(val value: P)` and `object Bar` |

For sum types, add `@file:OptIn(ExperimentalSerializationApi::class)` at the top of the file.

## Workspace Config

- Kotlin 2.1.21, KSP 2.1.21-2.0.1, kotlinx-serialization-json 1.8.1, JUnit 5.12.2
- All modules use JVM toolchain 21
- Package: `io.github.mcbianconi.quintkonnect`
- `QuintJson = Json { ignoreUnknownKeys = true }` used for all ITF deserialization
