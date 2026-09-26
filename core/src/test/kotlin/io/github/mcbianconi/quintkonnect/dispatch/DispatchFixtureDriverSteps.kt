package io.github.mcbianconi.quintkonnect.dispatch

import io.github.mcbianconi.quintkonnect.Step

// Hand-written stand-in for what `ksp`'s StepMethodGenerator would emit for DispatchFixtureDriver.
fun DispatchFixtureDriver.generatedStep(step: Step) {
    when (step.actionTaken) {
        "foo" -> lastAction = "foo"
        else -> error("Unimplemented action: ${step.actionTaken}")
    }
}
