package io.github.mcbianconi.quintkonnect

interface Driver {
    fun step(step: Step)

    fun config(): DriverConfig = DriverConfig()

    fun quintState(): State<*> = State.disabled<Driver>()
}
