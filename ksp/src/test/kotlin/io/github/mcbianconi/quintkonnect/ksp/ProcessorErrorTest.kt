@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.mcbianconi.quintkonnect.Driver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProcessorErrorTest {

    // qk-nqry: two @QuintAction methods sharing an action name must be a compile error.
    @Test
    fun `duplicate action names should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "DuplicateActionDriver.kt",
                """
                package dup

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class DuplicateActionDriver : Driver {
                    var lastAction = ""

                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("dup")
                    fun first() {
                        lastAction = "first"
                    }

                    @QuintAction("dup")
                    fun second() {
                        lastAction = "second"
                    }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("first"), result.messages)
        assertTrue(result.messages.contains("second"), result.messages)
    }

    // qk-9lsz: two @QuintAction methods sharing an action name must be rejected even when one is
    // inherited unchanged from an abstract base rather than declared on the driver itself.
    @Test
    fun `inherited duplicate action names should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "InheritedDuplicateActionDriver.kt",
                """
                package dupinherit

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                abstract class BaseDriver : Driver {
                    @QuintAction("dup")
                    fun first() {}
                }

                @QuintRun(spec = "unused.qnt")
                class InheritedDuplicateActionDriver : BaseDriver() {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("dup")
                    fun second() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("first"), result.messages)
        assertTrue(result.messages.contains("second"), result.messages)
    }

    // qk-c7z8: before this check, the second kind's generated files collided with the first's.
    @Test
    fun `driver with both QuintRun and QuintTest should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "BothKindsDriver.kt",
                """
                package bothkinds

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun
                import io.github.mcbianconi.quintkonnect.annotations.QuintTest

                @QuintRun(spec = "unused.qnt")
                @QuintTest(spec = "unused.qnt", test = "t")
                class BothKindsDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("a")
                    fun a() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(
            result.messages.contains("BothKindsDriver has both @QuintRun and @QuintTest"),
            result.messages,
        )
        assertTrue(!result.messages.contains("FileAlreadyExists"), result.messages)
    }

    // qk-9lsz: a @QuintAction function must be public, since the generated dispatcher calls it
    // from a separate file.
    @Test
    fun `internal QuintAction function should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "InternalActionDriver.kt",
                """
                package internalaction

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class InternalActionDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("hidden")
                    internal fun hidden() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("hidden"), result.messages)
    }

    @Test
    fun `private QuintAction function should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "PrivateActionDriver.kt",
                """
                package privateaction

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class PrivateActionDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("secret")
                    private fun secret() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("secret"), result.messages)
    }

    // qk-9lsz: `Runner.runTest`'s generated `driverFactory = { X() }` call needs a public no-arg
    // constructor; report that at KSP time rather than as a confusing error in generated code.
    @Test
    fun `driver with a private constructor should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "PrivateConstructorDriver.kt",
                """
                package privatector

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class PrivateConstructorDriver private constructor() : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("PrivateConstructorDriver"), result.messages)
    }

    @Test
    fun `driver with a required constructor argument should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "RequiredArgDriver.kt",
                """
                package requiredarg

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class RequiredArgDriver(val seed: Long) : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("RequiredArgDriver"), result.messages)
    }

    // qk-9lsz: a driver that inherits @QuintAction functions from an abstract base (some
    // untouched, one overridden without re-annotating) must compile and dispatch correctly.
    @Test
    fun `driver inheriting actions from an abstract base compiles and dispatches`() {
        val result = compileWithProcessor(
            kotlinSource(
                "InheritingDriver.kt",
                """
                package inheriting

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                abstract class BaseDriver : Driver {
                    var lastAction: String = ""

                    @QuintAction("init")
                    open fun init() {
                        lastAction = "base-init"
                    }

                    @QuintAction("untouched")
                    fun untouched() {
                        lastAction = "base-untouched"
                    }
                }

                @QuintRun(spec = "unused.qnt")
                class InheritingDriver : BaseDriver() {
                    override fun step(step: Step) = generatedStep(step)

                    override fun init() {
                        lastAction = "derived-init"
                    }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val driverClass = result.classLoader.loadClass("inheriting.InheritingDriver")
        val driver = driverClass.getDeclaredConstructor().newInstance() as Driver
        val lastActionField = driverClass.superclass.getDeclaredField("lastAction")
            .apply { isAccessible = true }

        driver.step(testStep("untouched"))
        assertEquals("base-untouched", lastActionField.get(driver))

        driver.step(testStep("init"))
        assertEquals("derived-init", lastActionField.get(driver))
    }

    // qk-hpzp: `quintState()` returning a `State` typed for a different driver must be a compile
    // error, since `ReplayRunner.runTest<D>` casts it to `State<D>` unchecked.
    @Test
    fun `quintState returning a State for a different driver should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "StateMismatchDriver.kt",
                """
                package statemismatch

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.State
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.TypedState
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun
                import kotlinx.serialization.builtins.serializer

                class OtherDriver : Driver {
                    override fun step(step: Step) {}
                }

                class OtherState : TypedState<OtherDriver, Long>(Long.serializer()) {
                    override fun extractFromDriver(driver: OtherDriver): Long = 0L
                }

                @QuintRun(spec = "unused.qnt")
                class StateMismatchDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    override fun quintState(): State<OtherDriver> = OtherState()

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("StateMismatchDriver"), result.messages)
        assertTrue(result.messages.contains("quintState"), result.messages)
    }

    // qk-hpzp: an explicit `State<*>` return type erases which driver the state belongs to, so the
    // processor can't prove the cast in `ReplayRunner` sound; require a narrower return type.
    @Test
    fun `quintState declared as State star should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "StarStateDriver.kt",
                """
                package starstate

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.State
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.TypedState
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun
                import kotlinx.serialization.builtins.serializer

                class StarStateDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    override fun quintState(): State<*> = StarState()

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
            kotlinSource(
                "StarState.kt",
                """
                package starstate

                import io.github.mcbianconi.quintkonnect.TypedState
                import kotlinx.serialization.builtins.serializer

                class StarState : TypedState<StarStateDriver, Long>(Long.serializer()) {
                    override fun extractFromDriver(driver: StarStateDriver): Long = 0L
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("StarStateDriver"), result.messages)
    }

    // qk-hpzp: a driver whose quintState() correctly names its own class must still compile fine,
    // whether the return type is explicit or left for the compiler to infer from the body.
    @Test
    fun `quintState correctly typed for its own driver compiles`() {
        val result = compileWithProcessor(
            kotlinSource(
                "MatchingStateDriver.kt",
                """
                package matchingstate

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.State
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.TypedState
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun
                import kotlinx.serialization.builtins.serializer

                class MatchingState : TypedState<MatchingStateDriver, Long>(Long.serializer()) {
                    override fun extractFromDriver(driver: MatchingStateDriver): Long = 0L
                }

                @QuintRun(spec = "unused.qnt")
                class MatchingStateDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    override fun quintState(): State<MatchingStateDriver> = MatchingState()

                    @QuintAction("go")
                    fun go() {}
                }

                // Same shape again, but with the return type left implicit: the processor must
                // resolve the compiler-inferred type, not just an explicit annotation.
                class InferredState : TypedState<InferredStateDriver, Long>(Long.serializer()) {
                    override fun extractFromDriver(driver: InferredStateDriver): Long = 0L
                }

                @QuintRun(spec = "unused.qnt")
                class InferredStateDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    override fun quintState() = InferredState()

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    // qk-hpzp: the coordinator flagged typealiases as the case most likely to defeat
    // isAssignableFrom-based resolution; a driver's return type can be a typealias for its own
    // State as long as it resolves to the driver's own class.
    @Test
    fun `quintState declared via a typealias for its own driver's State compiles`() {
        val result = compileWithProcessor(
            kotlinSource(
                "AliasStateDriver.kt",
                """
                package aliasstate

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.State
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.TypedState
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun
                import kotlinx.serialization.builtins.serializer

                typealias AliasState = State<AliasStateDriver>

                class AliasStateImpl : TypedState<AliasStateDriver, Long>(Long.serializer()) {
                    override fun extractFromDriver(driver: AliasStateDriver): Long = 0L
                }

                @QuintRun(spec = "unused.qnt")
                class AliasStateDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    override fun quintState(): AliasState = AliasStateImpl()

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    // The same typealias resolution must still reject a mismatch, not just wave it through
    // because the return type is a typealias rather than a plain State<...>.
    @Test
    fun `quintState declared via a typealias for a different driver's State should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "WrongAliasStateDriver.kt",
                """
                package wrongaliasstate

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.State
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.TypedState
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun
                import kotlinx.serialization.builtins.serializer

                class OtherDriver : Driver {
                    override fun step(step: Step) {}
                }

                typealias WrongAliasState = State<OtherDriver>

                class OtherStateImpl : TypedState<OtherDriver, Long>(Long.serializer()) {
                    override fun extractFromDriver(driver: OtherDriver): Long = 0L
                }

                @QuintRun(spec = "unused.qnt")
                class WrongAliasStateDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    override fun quintState(): WrongAliasState = OtherStateImpl()

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("WrongAliasStateDriver"), result.messages)
    }

    // qk-hpzp: a subclass that inherits quintState() from an abstract base sharing one State
    // across driver subclasses must still compile: the base's driver type is a supertype of the
    // subclass, which is exactly the case `ReplayRunner`'s cast needs to be sound.
    @Test
    fun `quintState inherited from a base driver typed for the base compiles`() {
        val result = compileWithProcessor(
            kotlinSource(
                "SharedStateDriver.kt",
                """
                package sharedstate

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.State
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.TypedState
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun
                import kotlinx.serialization.builtins.serializer

                abstract class BaseDriver : Driver {
                    override fun quintState(): State<BaseDriver> = BaseState()
                }

                class BaseState : TypedState<BaseDriver, Long>(Long.serializer()) {
                    override fun extractFromDriver(driver: BaseDriver): Long = 0L
                }

                @QuintRun(spec = "unused.qnt")
                class SharedStateDriver : BaseDriver() {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    // A driver that never overrides quintState() at all uses Driver's own disabled default, which
    // is always sound and must not be flagged.
    @Test
    fun `driver without a quintState override compiles`() {
        val result = compileWithProcessor(
            kotlinSource(
                "NoStateDriver.kt",
                """
                package nostate

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class NoStateDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    // qk-33ky: a suspend @QuintAction needs kotlinx.coroutines.runBlocking on the driver module's
    // own classpath; without it, the processor must report a clear error instead of letting the
    // generated dispatcher fail with an unresolved reference the user never wrote.
    @Test
    fun `suspend QuintAction without kotlinx-coroutines on the classpath should be rejected by the processor`() {
        val result = compileWithProcessorWithoutCoroutines(
            kotlinSource(
                "NoCoroutinesSuspendDriver.kt",
                """
                package nocoroutines

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class NoCoroutinesSuspendDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("go")
                    suspend fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("runBlocking"), result.messages)
        assertTrue(result.messages.contains("kotlinx-coroutines-core"), result.messages)
    }
}
