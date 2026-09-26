package io.github.mcbianconi.quintkonnect

public interface Driver {
    public fun step(step: Step)

    public fun config(): DriverConfig = DriverConfig()

    public fun quintState(): State<*> = State.disabled<Driver>()
}
