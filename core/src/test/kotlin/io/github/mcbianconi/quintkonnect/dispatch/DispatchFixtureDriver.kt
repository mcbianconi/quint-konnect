package io.github.mcbianconi.quintkonnect.dispatch

import io.github.mcbianconi.quintkonnect.Driver

// Stands in for a KSP-annotated driver: `DispatchFixtureDriverSteps.kt` next to this file plays
// the role of the KSP-generated `XSteps.kt`. This class relies on `Driver.step`'s default
// implementation (no `override fun step`) to reach it, exercising the class-name lookup in
// Driver.kt without running the KSP processor.
class DispatchFixtureDriver : Driver {
    var lastAction: String = ""
}
