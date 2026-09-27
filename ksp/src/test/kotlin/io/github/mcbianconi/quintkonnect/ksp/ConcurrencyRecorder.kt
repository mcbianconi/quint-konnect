package io.github.mcbianconi.quintkonnect.ksp

import java.util.concurrent.CopyOnWriteArrayList

// Loaded by this module's own (parent) classloader, so a kotlin-compile-testing fixture driver
// compiled with inheritClassPath = true resolves this same singleton, letting
// ParallelDynamicTestExecutionTest observe timing recorded from inside generated, dynamically
// compiled test code. Public, not internal: kotlin-compile-testing compiles that fixture as its
// own module, so Kotlin's module-scoped `internal` visibility would reject the reference even
// though both classes end up on the same classpath.
object ConcurrencyRecorder {

    data class Interval(val startNanos: Long, val endNanos: Long, val threadName: String)

    private val intervals = CopyOnWriteArrayList<Interval>()

    fun record(startNanos: Long, endNanos: Long, threadName: String) {
        intervals.add(Interval(startNanos, endNanos, threadName))
    }

    fun reset() {
        intervals.clear()
    }

    fun recorded(): List<Interval> = intervals.toList()

    fun anyOverlap(): Boolean {
        val snapshot = recorded()
        return snapshot.indices.any { i ->
            (i + 1 until snapshot.size).any { j ->
                val a = snapshot[i]
                val b = snapshot[j]
                a.startNanos < b.endNanos && b.startNanos < a.endNanos
            }
        }
    }
}
