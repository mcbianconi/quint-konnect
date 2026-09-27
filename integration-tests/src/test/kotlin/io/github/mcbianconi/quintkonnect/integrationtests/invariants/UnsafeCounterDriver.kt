package io.github.mcbianconi.quintkonnect.integrationtests.invariants

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.Step

// Not annotated with @QuintRun: this driver is only exercised via a direct RunConfig in
// UnsafeCounterInvariantTest, so no KSP dispatcher is generated for it and `step` is overridden
// directly (see Driver.step's KDoc). Replay never actually reaches it: the spec's `safe` invariant
// is violated before any trace is generated.
class UnsafeCounterDriver : Driver {
    override fun step(step: Step) {}
}
