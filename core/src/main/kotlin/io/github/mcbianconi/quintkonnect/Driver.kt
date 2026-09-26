package io.github.mcbianconi.quintkonnect

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

public interface Driver {
    /**
     * Dispatches [step] to the right `@QuintAction`-annotated method.
     *
     * The default implementation looks up the KSP-generated dispatcher for this driver's runtime
     * class (a driver annotated `@QuintRun`/`@QuintTest` in package `p` named `X` gets a generated
     * `p.XStepsKt.generatedStep(X, Step)`, from `ksp`'s `StepMethodGenerator`) and calls it, so a
     * driver normally doesn't need to override this at all. Override it only for custom dispatch;
     * `generatedStep` itself is still generated, so `override fun step(step: Step) =
     * generatedStep(step)` keeps compiling for drivers written that way.
     */
    public fun step(step: Step): Unit = dispatchGeneratedStep(this, step)

    public fun config(): DriverConfig = DriverConfig()

    public fun quintState(): State<*> = State.disabled<Driver>()
}

// Caching by Class (rather than e.g. a ConcurrentHashMap<Class<*>, Method>) ties the cached
// Method's lifetime to the driver class itself, so it doesn't pin a classloader that would
// otherwise be collected (relevant for kotlin-compile-testing, which spins up a fresh classloader
// per compilation).
private val generatedStepMethods: ClassValue<Method> = object : ClassValue<Method>() {
    override fun computeValue(driverClass: Class<*>): Method = resolveGeneratedStepMethod(driverClass)
}

private fun dispatchGeneratedStep(driver: Driver, step: Step) {
    val method = generatedStepMethods.get(driver.javaClass)
    try {
        method.invoke(null, driver, step)
    } catch (e: InvocationTargetException) {
        throw e.targetException
    }
}

private fun resolveGeneratedStepMethod(driverClass: Class<*>): Method {
    val generatedClassName = generatedStepsClassName(driverClass)
    val generatedClass = try {
        Class.forName(generatedClassName, true, driverClass.classLoader)
    } catch (e: ClassNotFoundException) {
        throw missingGeneratedStepError(driverClass, generatedClassName, e)
    }
    return try {
        generatedClass.getDeclaredMethod("generatedStep", driverClass, Step::class.java)
    } catch (e: NoSuchMethodException) {
        throw missingGeneratedStepError(driverClass, generatedClassName, e)
    }
}

private fun generatedStepsClassName(driverClass: Class<*>): String {
    val packageName = driverClass.packageName
    val prefix = if (packageName.isEmpty()) "" else "$packageName."
    return "$prefix${driverClass.simpleName}StepsKt"
}

private fun missingGeneratedStepError(
    driverClass: Class<*>,
    generatedClassName: String,
    cause: Throwable,
): IllegalStateException = IllegalStateException(
    "No generated step dispatcher found for ${driverClass.name}. Expected " +
        "$generatedClassName.generatedStep(${driverClass.simpleName}, Step) — did the ksp Gradle " +
        "plugin process this driver (@QuintRun/@QuintTest with at least one @QuintAction " +
        "function)? Otherwise override `step` yourself.",
    cause,
)
