# quint-konnect

quint-konnect replays [Quint](https://github.com/informalsystems/quint) traces against Kotlin code. The `quint` CLI writes the traces. A driver runs each step. When the driver returns a state, the library compares that state with the spec state.

This library is a Kotlin port of [quint-connect](https://github.com/quint-co/quint-connect).

```
Quint spec → quint CLI → ITF trace files → quint-konnect → your Kotlin code
```

## Quick start

### Apply the plugin

The plugin and its marker artifact are on Maven Central under `io.github.mcbianconi`. The plugin is not on the Gradle Plugin Portal. See [Plugin distribution](docs/decisions/gradle-plugin-distribution.md).

Add both repositories to `settings.gradle.kts`.

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Add this to `build.gradle.kts`. If the module already applies `kotlin("jvm")`, add the other two plugins to that block.

```kotlin
plugins {
    kotlin("jvm") version "2.4.20"
    id("io.github.mcbianconi.quint-konnect") version "0.2.0"
    kotlin("plugin.serialization") version "2.4.20"
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

Use Kotlin 2.4.20 or newer. The build needs JDK 21.

Applying the plugin has these effects.

- The plugin applies `com.google.devtools.ksp`.
- The plugin adds `kspTest("io.github.mcbianconi:quint-konnect-ksp")` and `testImplementation("io.github.mcbianconi:quint-konnect-core")` at the plugin's version.
- `quint-konnect-core` depends on `kotlinx-serialization-json`.
- The plugin adds `build/generated/ksp/test/kotlin` to the test source set.
- The plugin calls `useJUnitPlatform()` on every `Test` task.
- The plugin sets a system property so a relative `spec` path resolves against the Gradle project directory.
- That property applies to a test JVM that Gradle starts. An IDE test runner that does not use Gradle resolves `spec` against its own working directory.
- The plugin registers `checkQuint`. The task fails the build when `quint` is not on `PATH`. When the installed version differs from `quintKonnect.quintVersion`, the task warns and the build continues.
- The default `quintVersion` is `0.32.0`. The tests in this repository use that version.
- A `Test` task depends on `checkQuint` when the project has a `@QuintRun` or `@QuintTest` driver, or when `generateTraces` is true.
- A run with `-Pquint.replay` does not depend on `checkQuint`. When `generateTraces` is false, a project with no drivers does not depend on `checkQuint` either.
- The plugin registers `generateQuintTraces`. Leave `generateTraces` unset. `Test` tasks then do not depend on that task.
- The default runs `quint` during the test. A `--tests` filter then starts `quint` only for the drivers that run.
- Set `generateTraces` to `true`. The task then runs `quint` once per driver before `test`. The task writes the traces to `build/quint-konnect/traces/`. The tests replay those files.
- `generateQuintTraces`, `quintIr`, and trace generation during the test each stop after 10 minutes.

Set `quintVersion` only when you want a version other than `0.32.0`.

The plugin configures `testLogging` on every `Test` task. A failure prints the trace, the step, the action, and the diff. The output also includes the reproduce command and the replay command. Set `configureTestLogging` to `false` to keep your own logging. A `tasks.test` block placed below the `plugins` block also replaces that logging.

### Download quint

`downloadQuint` defaults to `false`. The build runs the `quint` program on `PATH`.

Set `downloadQuint` to `true` to download the pinned `quintVersion` binary. The `downloadQuint` task fetches that binary from the Quint GitHub releases. The file path is `<gradleUserHome>/caches/quint-konnect/quint/<version>/<os-arch>/quint`. The task checks the file checksum when a checksum is known for that version. `checkQuint` and every `Test` task that runs `quint` use that binary.

The download runs on macOS and Linux. See [No Windows support](docs/decisions/no-windows-support.md).

```kotlin
quintKonnect {
    downloadQuint.set(true)
}
```

### JUnit versions

This repository tests on JUnit 6.1.3. The plugin adds no JUnit dependency. Use any JUnit Jupiter 5.x or 6.x release. The generated adapter calls `@TestFactory` and `DynamicTest.dynamicTest(String, Executable)`. Those two calls are unchanged from JUnit Jupiter 5.0 through 6.x. JUnit 6 requires Java 17 and Kotlin 2.1. This project requires JDK 21 and Kotlin 2.4.20 or newer. See the [JUnit 6.0.0 release notes](https://docs.junit.org/6.0.0/release-notes/).

KSP always generates `<Driver>QuintSuite`. That object implements `QuintSuite`, holds the `RunConfig` or the `TestConfig`, and calls `ReplayRunner.traceReplays`. KSP also generates `<Driver>QuintRunTest` or `<Driver>QuintTestTest`. Set the adapter option to `none` to skip that class. Call `<Driver>QuintSuite.traceReplays()` from your test code.

```kotlin
ksp {
    arg("quintkonnect.adapter", "none")
}
```

`junit` is the default when the option is absent or blank. Any other value is a compile error.

<details>
<summary>Without the plugin</summary>

Use this build file when you do not apply the plugin. There is no `checkQuint` task. A missing `quint` program fails during trace generation. Set `quintkonnect.projectDir` yourself so a relative `spec` path resolves against the project directory.

```kotlin
plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    id("com.google.devtools.ksp") version "2.3.12"
}

dependencies {
    kspTest("io.github.mcbianconi:quint-konnect-ksp:0.2.0")
    testImplementation("io.github.mcbianconi:quint-konnect-core:0.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("quintkonnect.projectDir", project.projectDir.absolutePath)
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}
```

</details>

### Write a driver

Annotate a class with `@QuintRun` or `@QuintTest`. Implement `Driver`.

```kotlin
// src/test/kotlin/
@QuintRun(spec = "src/test/resources/my.qnt", maxSamples = 10)
class MyDriver : Driver {
    override fun quintState(): State<MyDriver> = MyState()

    @QuintAction("init")
    fun init() { /* reset implementation state */ }

    @QuintAction("Move")
    fun move(position: List<Long>) { /* execute move */ }

    @QuintAction("Undo")
    fun undo(position: List<Long>?) { /* nullable = optional nondet pick */ }
}
```

KSP generates `MyDriver.generatedStep`. That function calls the method whose `@QuintAction` name matches `step.actionTaken`. `Driver.step` finds `generatedStep` by class name. Keep the default `step`.

Return `State<MyDriver>` from `quintState`. KSP reports a compile error for any other type argument.

A `@QuintAction` function may be a `suspend` function. The generated code runs it with `runBlocking`. Add `kotlinx-coroutines-core` to the driver module when you use a `suspend` action.

### Compare state

```kotlin
@Serializable
data class MySpecState(val count: Long)

class MyState : TypedState<MyDriver, MySpecState>(serializer()) {
    override fun extractFromDriver(driver: MyDriver): MySpecState {
        return MySpecState(count = driver.counter)
    }
}
```

The library decodes the spec state from the ITF trace. It compares that value with `extractFromDriver` after each step. A mismatch names the fields that differ. One message is `cells.(1, 2): spec="X", impl="O"`. A set matches when both sides contain the same elements, in any order.

#### Let KSP generate the spec types

Set `readSpecIr` to `true`. KSP then generates `@Serializable` types from the spec.

```kotlin
quintKonnect {
    readSpecIr.set(true)
}
```

KSP writes `object <Module>Spec` in the driver package. The object contains the record types and the sum types. A `State` class has every state variable. Each anonymous record nondet has its own class. Each applied generic has its own class. `Opt[Coin]` becomes `OptCoin`.

You then write only `extractFromDriver`. Pass the generated state to `TypedState`.

```kotlin
TypedState<MyDriver, CounterSpec.State>(serializer())
```

KSP omits a spec type that it cannot decode, and every type that contains an omitted type. A tuple with mixed element types is one case. An uninterpreted type is another. KSP warns and lists each omitted type.

`ignore` on `@QuintRun` or `@QuintTest` names state variables to omit from a generated `<Driver>State` class. An unknown name is a compile error. Those variables are absent from that class. Use `ignore` when the implementation has no value to compare.

Put `@QuintIgnore` on a stored field to exclude that field from comparison. The field still decodes. Give the field a default when the spec can omit it. A KSP-generated class can be shared by several drivers in one module. Put `@QuintIgnore` only on a class you write.

Override `TypedState.compareField(path, spec, impl)` to compare one field yourself. Return `null` to keep the default comparison.

### Run the tests

```bash
./gradlew :mymodule:test
```

## Annotations

| Annotation | Target | Purpose |
|---|---|---|
| `@QuintRun` | class | Generates traces with `quint run --mbt` |
| `@QuintTest` | class | Generates traces with `quint test --match` |
| `@QuintAction("Name")` | method | Maps a Quint action to a driver method |

A driver takes one of `@QuintRun` and `@QuintTest`. KSP rejects a driver that has both.

`@QuintRun` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `spec` | String | required | Path to the `.qnt` file |
| `main` | String | `""` | Quint module name. Empty omits `--main`. |
| `init` | String | `""` | Init action. Empty omits `--init`. |
| `step` | String | `""` | Step action. Empty omits `--step`. |
| `maxSamples` | Int | `-1` | Number of traces. `-1` uses the library default of 100. |
| `maxSteps` | Int | `-1` | Maximum steps in one trace. `-1` omits `--max-steps`. Quint 0.32.0 uses 20 when the flag is absent. |
| `invariants` | `Array<String>` | `[]` | Names passed to `quint run --invariants`. A violation fails the test and reports the invariant name, the seed, and the trace. |
| `seed` | String | `""` | Fixed seed. Empty lets the library choose a random seed. |
| `ignore` | `Array<String>` | `[]` | State variables omitted from the generated `<Driver>State` class. An unknown name is a compile error. |

`@QuintTest` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `spec` | String | required | Path to the `.qnt` file |
| `test` | String | required | Test name passed to `quint test --match` |
| `main` | String | `""` | Quint module name. Empty omits `--main`. |
| `maxSamples` | Int | `-1` | Number of traces. `-1` uses the library default of 100. |
| `seed` | String | `""` | Fixed seed. Empty lets the library choose a random seed. |
| `ignore` | `Array<String>` | `[]` | State variables omitted from the generated `<Driver>State` class. An unknown name is a compile error. |

`quint test` does not write `mbt::actionTaken` or `mbt::nondetPicks`. Store the action in a sum-type state variable. Pass that variable's name in `DriverConfig.nondetPath`.

```kotlin
override fun config() = DriverConfig(nondetPath = listOf("lastAction"))
```

KSP does not read `config()`. Add the variable to `ignore` in the annotation when the implementation does not store it. The [counter driver](example/src/test/kotlin/io/github/mcbianconi/quintkonnect/example/quinttest/CounterDriver.kt) lists `lastAction` in `ignore`. See [Why @QuintTest needs nondetPath](docs/decisions/quint-test-needs-nondet-path.md).

## Quint types in Kotlin

The Quint-to-Kotlin type table is in [`skills/quint-konnect/references/types.md`](skills/quint-konnect/references/types.md). The table covers state fields and `@QuintAction` parameters. It includes sum types, tuples, maps, `Option[T]`, and integers that do not fit in 64 bits.

## ITF traces

ITF is the Informal Trace Format. Quint and [Apalache](https://apalache-mc.org) write execution traces as ITF JSON. A `#` key tags a Quint type that JSON cannot express.

| ITF JSON | Quint type | `ItfValue` variant |
|---|---|---|
| `true` or `false` | `bool` | `ItfValue.Bool` |
| `42` | `int` that fits in 64 bits | `ItfValue.Num` |
| `"hello"` | `str` | `ItfValue.Str` |
| `{"#bigint": "123"}` | `int` of any precision | `ItfValue.BigInt` |
| `[1, 2, 3]` | `List[int]` | `ItfValue.List` |
| `{"#tup": [1, 2]}` | `(int, int)` | `ItfValue.Tup` |
| `{"#set": [1, 2]}` | `Set[int]` | `ItfValue.Set` |
| `{"#map": [[1, "a"]]}` | `int -> V` | `ItfValue.Map` |
| `{"field": 1}` | record, or a sum-type variant | `ItfValue.Record` |

In `quint run --mbt`, each state also has two `mbt` variables.

- `mbt::actionTaken` is the Quint action that produced the state.
- `mbt::nondetPicks` is the record of nondeterministic choices for that action.

The library reads both values to select the driver method and to decode its parameters.

## Environment variables

| Variable | Values | Description |
|---|---|---|
| `QUINT_VERBOSE` | `0`, `1`, `2` | `0` is the default. `1` logs each trace and step. `2` also prints the raw ITF state. |
| `QUINT_SEED` | a seed string, for example `0xdeadbeef` | Fixes the random seed |
| `QUINT_COLOR` | `always`, `never` | Forces ANSI colours on or off. The default uses colours only in a terminal. |
| `NO_COLOR` | any non-empty value | Disables ANSI colours. See [no-color.org](https://no-color.org). When `QUINT_COLOR` is set, it overrides `NO_COLOR`. |

```bash
QUINT_VERBOSE=1 ./gradlew -p example test
QUINT_SEED=0x1234 ./gradlew -p example test
```

## Override a run from Gradle

`maxSamples`, `maxSteps`, and `seed` are fixed when KSP compiles the driver. These Gradle properties replace those values for one `test` run. The driver does not need a new compilation.

The seed is chosen in this order. `-Pquint.seed` is first. A non-empty `seed` on `@QuintRun` or `@QuintTest` is second. `QUINT_SEED` is third. A random seed is last.

| Gradle property | System property | Effect |
|---|---|---|
| `-Pquint.maxSamples=<int>` | `quintkonnect.maxSamples` | Replaces `maxSamples` on `@QuintRun` or `@QuintTest`. When the property is absent, the annotation value applies. `-1` means 100. |
| `-Pquint.maxSteps=<int>` | `quintkonnect.maxSteps` | Replaces `maxSteps` on `@QuintRun`. When the property is absent, the annotation value applies. `-1` omits `--max-steps`. `quint test` has no `--max-steps`. |
| `-Pquint.seed=<hex>` | `quintkonnect.seed` | Replaces the seed. See the order above. |
| `-Pquint.verbose=0`, `1`, or `2` | `quintkonnect.verbose` | Replaces `ConsoleReplayListener` verbosity. When the property is absent, `QUINT_VERBOSE` applies. When that variable is also absent, the listener uses 0. |
| `-Pquint.replay=<path>` | `quintkonnect.replay` | Replays a saved `.itf.json` file or directory. See [Replay a saved trace](#replay-a-saved-trace). |
| `-Pquint.parallelism=<int>` | `quintkonnect.parallelism` | Pool size for `Runner.runTest`. The default is 1. See [Run traces in parallel](#run-traces-in-parallel). |

```bash
./gradlew -p example test -Pquint.maxSamples=1000 -Pquint.verbose=1
./gradlew -p example test -Pquint.maxSamples=20
```

Each property is an input of the `Test` task. A change runs the tests again. The build fails at configuration time when `-Pquint.maxSamples` or `-Pquint.maxSteps` is not an integer. The build fails at configuration time when `-Pquint.verbose` is not 0, 1, or 2. The failure message names the values the property accepts.

## Replay a saved trace

A seed reproduces a failure only with the same spec and the same `quint` version. The library deletes the temporary `.itf.json` file when the run ends. On a failing trace, `ReplayRunner.traceReplays` saves the file and prints a command.

```
Saved failing trace to build/quint-konnect/failures/TicTacToeDriver-trace3.itf.json
Replay it with:
   ./gradlew :test --tests '*TicTacToeDriver*' -Pquint.replay=build/quint-konnect/failures/TicTacToeDriver-trace3.itf.json
```

`-Pquint.replay` accepts a file or a directory of `.itf.json` files. The path resolves against the project directory. The run uses `ItfFileTraceSource` and does not call `quint`. `Test` tasks then skip `checkQuint` and `downloadQuint`. Construct `ItfFileTraceSource` yourself to replay a file from test resources without the Gradle plugin.

`readSpecIr` set to `true` still needs `quint`. The generated `<Module>Spec` types are part of the driver compilation. The build runs `quint` through `quintIr` to typecheck the spec and generate those types. See [Why replay still needs quint when readSpecIr is on](docs/decisions/replay-needs-quint-with-read-spec-ir.md).

A second failure of the same saved file does not write a new file. The printed command still names the input file.

### Shrink a failing trace

Run `shrinkQuintTraces` with the seed of the failing run. Filter to one driver.

```bash
./gradlew shrinkQuintTraces --tests '*MyDriver*' -Pquint.seed=<seed of the failing run>
```

For the first failing trace of a `@QuintRun` driver, the task runs `quint run` again with that seed. It tries `--max-steps` from 0 through one less than the failing step. It reports the first failing trace.

```
Shrunk failing trace: it failed at step 5; rerunning quint with the same seed (42) and --max-steps 2, trace 1 fails at step 2.
```

The task saves that trace as `build/quint-konnect/failures/<Driver>-shrunk-trace<N>.itf.json` and prints a replay command for the `test` task. The task runs `quint` itself. It does not replay output from `generateQuintTraces`. The task is never `UP-TO-DATE`. The task does not shrink a `@QuintTest` driver, an invariant violation, or a `-Pquint.replay` run.

## Run traces in parallel

Each trace uses a new driver instance. Generated tests and `Runner.runTest` do not share one thread pool.

The generated `traces` function has `@Execution(ExecutionMode.CONCURRENT)`. KSP skips that annotation when `quintkonnect.adapter` is `none`. KSP skips the adapter class in that case too. Enable JUnit parallel execution. The dynamic tests then run at the same time. Add `src/test/resources/junit-platform.properties`.

```properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=4
```

`junit.jupiter.execution.parallel.mode.default` and `junit.jupiter.execution.parallel.mode.classes.default` both default to `same_thread`. Other `@Test` methods in the module stay on one thread until you change those properties. JUnit has no separate default for dynamic tests. A dynamic test uses the execution mode of its `@TestFactory` method. See [JUnit parallel execution](https://docs.junit.org/current/user-guide/#writing-tests-parallel-execution).

`ConsoleReplayListener` stores one trace's lines and prints them together when that trace ends.

When `junit.jupiter.execution.parallel.enabled` is absent or `false`, JUnit runs every test on the calling thread. The `@Execution` annotation then has no effect.

### Run Runner.runTest on a thread pool

Set `-Pquint.parallelism` to the pool size. The system property is `quintkonnect.parallelism`. The default is 1. The property changes only `Runner.runTest`. Generated tests do not read the property. When a trace fails, the run throws the failure with the lowest trace index. Every trace finishes before that failure is thrown.

## Modules

| Module | Description |
|---|---|
| `annotations` | Annotation declarations. No runtime dependency. |
| `itf` | ITF parsing, and decoding into `@Serializable` types. The types are `ItfValue` and `ItfTrace`. |
| `core` | Runtime. Invokes the `quint` CLI, generates traces, reads steps, compares state, and runs replays. |
| `ksp` | KSP2 processor. Generates `generatedStep`, a `QuintSuite` object, and a JUnit adapter class with one dynamic test per trace. |
| `gradle-plugin` | Plugin id `io.github.mcbianconi.quint-konnect`. Applies KSP, adds the dependencies, and registers `checkQuint`, `downloadQuint`, `quintIr`, `generateQuintTraces`, and `shrinkQuintTraces`. |
| `example` | End-to-end examples in a separate Gradle build. See [`example/README.md`](example/README.md). |
| `integration-tests` | Regression tests against a real `quint` binary. Not published. |

## Example

[`example/`](example) is a TicTacToe project. It applies the plugin as the quick start does. Run `./gradlew -p example build`.

- [`TicTacToe.kt`](example/src/main/kotlin/io/github/mcbianconi/quintkonnect/example/tictactoe/TicTacToe.kt) is the game.
- [`TicTacToeDriver.kt`](example/src/test/kotlin/io/github/mcbianconi/quintkonnect/example/tictactoe/TicTacToeDriver.kt) is the driver.
- [`TicTacToeState.kt`](example/src/test/kotlin/io/github/mcbianconi/quintkonnect/example/tictactoe/TicTacToeState.kt) compares state with the generated `TictactoeSpec` types.
- [`tictactoe.qnt`](example/src/test/resources/tictactoe.qnt) is the Quint spec.

More drivers live under the [example test sources](example/src/test/kotlin/io/github/mcbianconi/quintkonnect/example).

- `asyncstore/` is an async bank ledger. It uses a `Mutex`, a simulated delay, and `suspend` actions.
- `sumtypes/` is a vending machine. The spec stores the action in a sum type and nests the state in a record. The driver sets `statePath` and `nondetPath` on `DriverConfig`.
- `projection/` is a warehouse. The driver uses `@QuintIgnore` and `compareField`. A second driver fails on purpose.
- `quinttest/`, `suspending/`, `partialstate/`, and `buggy/` are smaller fixtures. They cover `@QuintTest` with `ignore`, `suspend` actions, `@QuintIgnore`, and a negative test.

## Install the agent skill

The skill is [`skills/quint-konnect/SKILL.md`](skills/quint-konnect/SKILL.md). Follow it when an agent adds quint-konnect to a Kotlin project. The skill covers the Gradle setup, the driver, `TypedState`, and debugging with `QUINT_SEED` and `QUINT_VERBOSE`.

Install the skill with one of these commands.

- Claude Code. Run `/plugin marketplace add mcbianconi/quint-konnect`. Then run `/plugin install quint-konnect@quint-konnect`.
- Other agents, through [`npx skills`](https://github.com/vercel-labs/agent-skills): `npx skills add mcbianconi/quint-konnect --skill quint-konnect`.

The `--skill` filter is required. This repository also has dev skills under `.claude/skills/`. Without the filter, `npx skills` installs those too. See [Agent skill packaging](docs/decisions/agent-skill-plugin.md).

## Build this repository

```bash
./gradlew build
./gradlew -p example build
```

`./gradlew build` builds the modules in this repository. `./gradlew -p example build` builds `example` and runs its tests. The example build needs the `quint` program on `PATH`. `AGENTS.md` lists the other tasks, including `spotlessApply`.

## License

Apache-2.0. See [LICENSE](LICENSE). This is a port of [quint-connect](https://github.com/quint-co/quint-connect), also Apache-2.0.
